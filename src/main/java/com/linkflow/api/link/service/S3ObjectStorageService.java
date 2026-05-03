package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Service
public class S3ObjectStorageService {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private final boolean enabled;
    private final String bucket;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String publicBaseUrl;
    private final String prefix;
    private final HttpClient httpClient;
    private final Clock clock;

    @Autowired
    public S3ObjectStorageService(
            @Value("${app.storage.qrcode.s3.enabled:false}") boolean enabled,
            @Value("${app.storage.qrcode.s3.bucket:}") String bucket,
            @Value("${app.storage.qrcode.s3.region:}") String region,
            @Value("${app.storage.qrcode.s3.access-key:${AWS_ACCESS_KEY_ID:}}") String accessKey,
            @Value("${app.storage.qrcode.s3.secret-key:${AWS_SECRET_ACCESS_KEY:}}") String secretKey,
            @Value("${app.storage.qrcode.s3.endpoint:}") String endpoint,
            @Value("${app.storage.qrcode.s3.public-base-url:}") String publicBaseUrl,
            @Value("${app.storage.qrcode.s3.prefix:qrcodes}") String prefix
    ) {
        this(enabled, bucket, region, accessKey, secretKey, endpoint, publicBaseUrl, prefix, Clock.systemUTC());
    }

    S3ObjectStorageService(
            boolean enabled,
            String bucket,
            String region,
            String accessKey,
            String secretKey,
            String endpoint,
            String publicBaseUrl,
            String prefix,
            Clock clock
    ) {
        this.enabled = enabled;
        this.bucket = normalize(bucket);
        this.region = normalize(region);
        this.accessKey = normalize(accessKey);
        this.secretKey = normalize(secretKey);
        this.endpoint = stripTrailingSlash(normalize(endpoint));
        this.publicBaseUrl = stripTrailingSlash(normalize(publicBaseUrl));
        this.prefix = stripSlashes(normalize(prefix) == null ? "qrcodes" : normalize(prefix));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.clock = clock;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 上传 PNG 到 S3 兼容对象存储
     *
     * 这里手写 AWS Signature V4；Java HttpClient 不能手动设置 Host header。
     */
    public StoredObject putPng(String key, byte[] bytes) {
        ensureConfigured();
        String normalizedKey = stripSlashes(key);
        URI uri = objectUri(normalizedKey);
        Instant now = clock.instant();
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String payloadHash = sha256Hex(bytes);
        String host = uri.getHost();
        String canonicalUri = uri.getRawPath();
        String canonicalHeaders = "content-type:image/png\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = "PUT\n"
                + canonicalUri + "\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String signature = HexFormat.of().formatHex(
                hmac(signingKey(dateStamp), stringToSign)
        );

        String authorization = "AWS4-HMAC-SHA256 "
                + "Credential=" + accessKey + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", "
                + "Signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "image/png")
                .header("x-amz-content-sha256", payloadHash)
                .header("x-amz-date", amzDate)
                .header("Authorization", authorization)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "S3_UPLOAD_FAILED",
                        "QR code image upload to S3 failed.",
                        Map.of("status", response.statusCode(), "bucket", bucket)
                );
            }
            return new StoredObject(normalizedKey, publicUrl(uri, normalizedKey));
        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "S3_UPLOAD_FAILED",
                    "QR code image upload to S3 failed.",
                    Map.of("error", ex.getClass().getSimpleName())
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "S3_UPLOAD_FAILED",
                    "QR code image upload to S3 was interrupted.",
                    Map.of()
            );
        }
    }

    /**
     * 生成二维码资产的对象存储 key
     */
    public String qrCodeKey(String publicId, int size) {
        return prefix + "/" + publicId + "/" + size + ".png";
    }

    private void ensureConfigured() {
        if (!enabled) {
            throw new IllegalStateException("S3 storage is disabled");
        }
        if (bucket == null || region == null || accessKey == null || secretKey == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "S3_NOT_CONFIGURED",
                    "S3 QR code storage is enabled but bucket, region, or credentials are missing.",
                    Map.of()
            );
        }
    }

    private URI objectUri(String key) {
        String encodedKey = encodeKey(key);
        if (endpoint != null) {
            return URI.create(endpoint + "/" + bucket + "/" + encodedKey);
        }
        return URI.create("https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey);
    }

    private String publicUrl(URI uploadUri, String key) {
        if (publicBaseUrl != null) {
            return publicBaseUrl + "/" + encodeKey(key);
        }
        return uploadUri.toString();
    }

    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] dateRegionKey = hmac(dateKey, region);
        byte[] dateRegionServiceKey = hmac(dateRegionKey, "s3");
        return hmac(dateRegionServiceKey, "aws4_request");
    }

    private byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign S3 request", ex);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to hash S3 payload", ex);
        }
    }

    private String encodeKey(String key) {
        return java.util.Arrays.stream(key.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String stripTrailingSlash(String value) {
        return value == null ? null : value.replaceAll("/+$", "");
    }

    private String stripSlashes(String value) {
        return value == null ? null : value.replaceAll("^/+|/+$", "").toLowerCase(Locale.ROOT);
    }

    public record StoredObject(String key, String url) {
    }
}
