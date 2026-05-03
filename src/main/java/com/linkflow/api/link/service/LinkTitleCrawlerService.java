package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.dto.response.TitlePreviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LinkTitleCrawlerService {

    private static final int MAX_TITLE_LENGTH = 300;
    private static final int MAX_BODY_BYTES = 256 * 1024;
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?is)<title[^>]*>(.*?)</title>"
    );
    private static final Pattern CHARSET_PATTERN = Pattern.compile(
            "(?is)charset\\s*=\\s*['\"]?([a-zA-Z0-9._:-]+)"
    );
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final HttpClient httpClient;

    public LinkTitleCrawlerService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 解析最终标题
     *
     * 优先使用用户传入标题，其次抓取网页标题，最后用域名兜底。
     */
    public String resolveTitle(String longUrl, String requestedTitle) {
        String normalized = normalizeTitle(requestedTitle);
        if (normalized != null) {
            return normalized;
        }
        return fetchTitle(longUrl)
                .map(TitlePreviewResponse::title)
                .orElseGet(() -> fallbackTitle(longUrl));
    }

    /**
     * 抓取网页标题
     *
     * 该方法只抓公共 http/https URL，并限制响应体大小，避免 SSRF 和资源消耗风险。
     */
    public Optional<TitlePreviewResponse> fetchTitle(String longUrl) {
        URI uri = validatePublicHttpUrl(longUrl);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "LinkFlowTitleCrawler/1.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("content-type").orElse("");
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !isHtml(contentType)) {
                return Optional.empty();
            }

            byte[] body = response.body();
            if (body.length > MAX_BODY_BYTES) {
                body = java.util.Arrays.copyOf(body, MAX_BODY_BYTES);
            }

            String html = new String(body, detectCharset(contentType).orElse(StandardCharsets.UTF_8));
            return extractTitle(html)
                    .map(title -> new TitlePreviewResponse(longUrl, title, "crawler"));
        } catch (IOException ex) {
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private URI validatePublicHttpUrl(String longUrl) {
        URI uri = URI.create(longUrl);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "URL_CRAWL_REJECTED",
                    "Only public http and https URLs can be crawled.",
                    Map.of("long_url", longUrl)
            );
        }

        String asciiHost = IDN.toASCII(uri.getHost());
        try {
            for (InetAddress address : InetAddress.getAllByName(asciiHost)) {
                if (!isPublicAddress(address)) {
                    throw rejectedPrivateUrl(longUrl);
                }
            }
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "URL_CRAWL_REJECTED",
                    "URL host could not be resolved.",
                    Map.of("long_url", longUrl)
            );
        }
        return uri;
    }

    private ApiException rejectedPrivateUrl(String longUrl) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "URL_CRAWL_REJECTED",
                "Private, loopback, link-local, and metadata URLs cannot be crawled.",
                Map.of("long_url", longUrl)
        );
    }

    private boolean isPublicAddress(InetAddress address) {
        byte[] raw = address.getAddress();
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        if (raw.length == 4) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            return !(first == 0
                    || first == 10
                    || first == 127
                    || first == 169 && second == 254
                    || first == 172 && second >= 16 && second <= 31
                    || first == 192 && second == 168);
        }

        String hex = HexFormat.of().formatHex(raw);
        return !(hex.startsWith("00000000000000000000ffff")
                || hex.startsWith("fc")
                || hex.startsWith("fd"));
    }

    private boolean isHtml(String contentType) {
        String value = contentType.toLowerCase(Locale.ROOT);
        return value.contains("text/html") || value.contains("application/xhtml+xml");
    }

    private Optional<Charset> detectCharset(String contentType) {
        var matcher = CHARSET_PATTERN.matcher(contentType);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(matcher.group(1)));
        } catch (Exception ex) {
            // 非法 charset 直接回退 UTF-8，避免外部站点错误配置影响创建链路。
            return Optional.empty();
        }
    }

    private Optional<String> extractTitle(String html) {
        var matcher = TITLE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.ofNullable(normalizeTitle(decodeHtml(matcher.group(1))));
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() <= MAX_TITLE_LENGTH
                ? normalized
                : normalized.substring(0, MAX_TITLE_LENGTH);
    }

    private String fallbackTitle(String longUrl) {
        URI uri = URI.create(longUrl);
        String host = uri.getHost() == null ? longUrl : uri.getHost();
        return host.replaceFirst("^www\\.", "");
    }

    private String decodeHtml(String value) {
        return value.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
