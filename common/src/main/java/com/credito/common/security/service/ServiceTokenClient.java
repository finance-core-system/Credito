package com.credito.common.security.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * client_credentials grant로 내부 service-token을 발급받는 HTTP client입니다.
 *
 * <p>Keycloak token endpoint에 form 요청을 보내고 access token, token type,
 * 만료 시간, scope를 응답 값 객체로 반환합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>client_credentials token 요청 생성</li>
 *     <li>scope 목록을 OAuth2 form 값으로 인코딩</li>
 *     <li>token endpoint 응답 파싱</li>
 *     <li>토큰 발급 실패 응답을 예외로 변환</li>
 * </ul>
 */
public class ServiceTokenClient {

    private static final TypeReference<Map<String, Object>> TOKEN_RESPONSE_TYPE = new TypeReference<>() {
    };

    private final URI tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ServiceTokenClient(URI tokenEndpoint, String clientId, String clientSecret) {
        this(tokenEndpoint, clientId, clientSecret, HttpClient.newHttpClient(), new ObjectMapper());
    }

    ServiceTokenClient(
        URI tokenEndpoint,
        String clientId,
        String clientSecret,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint, "token endpoint는 null일 수 없습니다.");
        this.clientId = requireText(clientId, "client id는 비어 있을 수 없습니다.");
        this.clientSecret = requireText(clientSecret, "client secret은 비어 있을 수 없습니다.");
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP client는 null일 수 없습니다.");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper는 null일 수 없습니다.");
    }

    public ServiceTokenResponse issueToken(Collection<String> scopes) {
        try {
            HttpRequest request = HttpRequest.newBuilder(tokenEndpoint)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form(scopes)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceTokenException("service-token 발급에 실패했습니다. status=" + response.statusCode());
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), TOKEN_RESPONSE_TYPE);
            String accessToken = requireText(
                stringValue(body.get("access_token")),
                "token endpoint 응답에 access_token이 없습니다.");
            String tokenType = requireText(
                stringValue(body.get("token_type")),
                "token endpoint 응답에 token_type이 없습니다.");
            long expiresIn = requirePositiveLong(
                body.get("expires_in"),
                "token endpoint 응답에 expires_in이 없거나 유효하지 않습니다.");
            return new ServiceTokenResponse(
                accessToken,
                tokenType,
                expiresIn,
                stringValue(body.get("scope")));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceTokenException("service-token 발급 요청이 중단되었습니다.", exception);
        } catch (Exception exception) {
            if (exception instanceof ServiceTokenException serviceTokenException) {
                throw serviceTokenException;
            }
            throw new ServiceTokenException("service-token 발급 요청을 처리할 수 없습니다.", exception);
        }
    }

    private String form(Collection<String> scopes) {
        String scope = scopes == null || scopes.isEmpty() ? null : String.join(" ", scopes);
        StringJoiner form = new StringJoiner("&");
        form.add(parameter("grant_type", "client_credentials"));
        form.add(parameter("client_id", clientId));
        form.add(parameter("client_secret", clientSecret));
        if (scope != null && !scope.isBlank()) {
            form.add(parameter("scope", scope));
        }
        return form.toString();
    }

    private static String parameter(String name, String value) {
        return encode(name) + "=" + encode(value);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static long requirePositiveLong(Object value, String message) {
        if (value instanceof Number number) {
            long parsed = number.longValue();
            if (parsed > 0) {
                return parsed;
            }
            throw new ServiceTokenException(message);
        }
        if (value == null) {
            throw new ServiceTokenException(message);
        }
        try {
            long parsed = Long.parseLong(value.toString());
            if (parsed > 0) {
                return parsed;
            }
            throw new ServiceTokenException(message);
        } catch (NumberFormatException exception) {
            throw new ServiceTokenException(message, exception);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ServiceTokenException(message);
        }
        return value;
    }

    /**
     * token endpoint 응답 값을 표현하는 값 객체입니다.
     *
     * <p>access token과 token type, 만료 시간, scope 문자열을 보관합니다.</p>
     */
    public record ServiceTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String scope
    ) {
    }
}
