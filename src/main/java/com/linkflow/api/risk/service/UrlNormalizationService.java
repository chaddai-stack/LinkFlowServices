package com.linkflow.api.risk.service;

import org.springframework.stereotype.Service;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UrlNormalizationService {

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    /**
     * 统一 URL 解析入口。
     *
     * 这里不发起网络请求，只做字符串级别的规范化和安全边界识别，
     * 后续 Hugging Face 或页面抓取 provider 都应该复用这层结果。
     */
    public UrlFacts parse(String rawUrl) {
        String trimmed = rawUrl == null ? "" : rawUrl.trim();
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = normalizeHost(uri.getHost());
            if (host == null || host.isBlank()) {
                return UrlFacts.invalid(trimmed, "missing_host");
            }

            URI normalized = new URI(
                    scheme,
                    uri.getRawUserInfo(),
                    host,
                    uri.getPort(),
                    blankToSlash(uri.getRawPath()),
                    uri.getRawQuery(),
                    null
            );
            return new UrlFacts(
                    trimmed,
                    normalized.toASCIIString(),
                    scheme,
                    host,
                    uri.getRawUserInfo() != null,
                    isIpLiteral(host),
                    isPrivateOrLocalHost(host),
                    queryParameterCount(uri.getRawQuery()),
                    true,
                    null
            );
        } catch (IllegalArgumentException | URISyntaxException ex) {
            return UrlFacts.invalid(trimmed, "invalid_url");
        }
    }

    public String normalizeDomain(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isBlank()) {
            return candidate;
        }

        if (candidate.contains("://")) {
            return parse(candidate).host();
        }

        return normalizeHost(candidate);
    }

    public String normalizeUrl(String value) {
        UrlFacts facts = parse(value);
        return facts.valid() ? facts.normalizedUrl() : value.trim();
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String withoutBrackets = host.strip().replace("[", "").replace("]", "");
        return IDN.toASCII(withoutBrackets.toLowerCase(Locale.ROOT));
    }

    private String blankToSlash(String path) {
        return path == null || path.isBlank() ? "/" : path;
    }

    private int queryParameterCount(String query) {
        if (query == null || query.isBlank()) {
            return 0;
        }
        return query.split("&", -1).length;
    }

    private boolean isIpLiteral(String host) {
        return IPV4.matcher(host).matches() || host.contains(":");
    }

    private boolean isPrivateOrLocalHost(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost")) {
            return true;
        }

        if (IPV4.matcher(host).matches()) {
            String[] parts = host.split("\\.");
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 0
                    || first == 10
                    || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        }

        String compact = host.toLowerCase(Locale.ROOT);
        return "::1".equals(compact)
                || compact.startsWith("fc")
                || compact.startsWith("fd")
                || compact.startsWith("fe80");
    }

    public record UrlFacts(
            String originalUrl,
            String normalizedUrl,
            String scheme,
            String host,
            boolean hasCredentials,
            boolean ipLiteral,
            boolean privateOrLocalHost,
            int queryParameterCount,
            boolean valid,
            String invalidReason
    ) {
        static UrlFacts invalid(String originalUrl, String reason) {
            return new UrlFacts(originalUrl, originalUrl, "", "", false, false, false, 0, false, reason);
        }
    }
}
