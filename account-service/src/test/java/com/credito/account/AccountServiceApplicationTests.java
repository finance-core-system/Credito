package com.credito.account;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "SERVER_PORT=0",
        "MTLS_ENABLED=false",
        "MTLS_KEY_STORE=",
        "MTLS_KEY_STORE_PASSWORD=",
        "MTLS_KEY_STORE_TYPE=PKCS12",
        "MTLS_TRUST_STORE=",
        "MTLS_TRUST_STORE_PASSWORD=",
        "MTLS_TRUST_STORE_TYPE=PKCS12",
        "MTLS_CLIENT_AUTH=none",
        "credito.security.resource-server.service-token.require-client-certificate=false"
    })
class AccountServiceApplicationTests {

    private static final String KEY_ID = "account-service-test-key";
    private static final KeyPair KEY_PAIR = createKeyPair();
    private static final RSAKey JWK = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) KEY_PAIR.getPublic())
        .privateKey((java.security.interfaces.RSAPrivateKey) KEY_PAIR.getPrivate())
        .keyID(KEY_ID)
        .build();
    private static final HttpServer JWK_SERVER = startJwkServer();
    private static final String ISSUER_URI = "http://localhost:" + JWK_SERVER.getAddress().getPort() + "/realms/customer-realm";
    private static final String SYSTEM_ISSUER_URI =
        "http://localhost:" + JWK_SERVER.getAddress().getPort() + "/realms/system-realm";
    private static final String JWK_SET_URI = "http://localhost:" + JWK_SERVER.getAddress().getPort() + "/jwks";

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void resourceServerProperties(DynamicPropertyRegistry registry) {
        registry.add("CUSTOMER_REALM_ISSUER_URI", () -> ISSUER_URI);
        registry.add("CUSTOMER_REALM_JWK_SET_URI", () -> JWK_SET_URI);
        registry.add("CUSTOMER_REALM_INTERNAL_ISSUER_URI", () -> "http://keycloak:8080/realms/customer-realm");
        registry.add("CUSTOMER_REALM_INTERNAL_JWK_SET_URI", () -> JWK_SET_URI);
        registry.add("ADMIN_REALM_ISSUER_URI", () -> "http://localhost:8085/realms/admin-realm");
        registry.add("ADMIN_REALM_JWK_SET_URI", () -> JWK_SET_URI);
        registry.add("ADMIN_REALM_INTERNAL_ISSUER_URI", () -> "http://keycloak:8080/realms/admin-realm");
        registry.add("ADMIN_REALM_INTERNAL_JWK_SET_URI", () -> JWK_SET_URI);
        registry.add("SYSTEM_REALM_ISSUER_URI", () -> SYSTEM_ISSUER_URI);
        registry.add("SYSTEM_REALM_JWK_SET_URI", () -> JWK_SET_URI);
        registry.add("SYSTEM_REALM_INTERNAL_ISSUER_URI", () -> "http://keycloak:8080/realms/system-realm");
        registry.add("SYSTEM_REALM_INTERNAL_JWK_SET_URI", () -> JWK_SET_URI);
    }

    @AfterAll
    static void stopJwkServer() {
        JWK_SERVER.stop(0);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void applicationEndpointsRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/accounts",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void authenticatedRequestWithValidJwtReachesApplicationEndpoint() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenWithAudience("credito-api"));

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/test/authenticated",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("authenticated", response.getBody());
    }

    @Test
    void jwtWithUnsupportedAudienceIsRejected() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenWithAudience("unknown-api"));

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/test/authenticated",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void serviceTokenWithAllowedClientAndScopeCanAccessAccountsApi() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceToken("gateway-service", "accounts.read"));

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/accounts",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("accounts", response.getBody());
    }

    @Test
    void serviceTokenWithMissingScopeIsRejected() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceToken("gateway-service", "customers.read"));

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/accounts",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private static String tokenWithAudience(String audience) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(ISSUER_URI)
            .subject("test-customer")
            .audience(audience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .jwtID(UUID.randomUUID().toString())
            .claim("roles", List.of("ROLE_CUSTOMER"))
            .build();

        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
            claimsSet);
        jwt.sign(new RSASSASigner(JWK));
        return jwt.serialize();
    }

    private static String serviceToken(String clientId, String scope) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(SYSTEM_ISSUER_URI)
            .subject("service-account-" + clientId)
            .audience("credito-internal-api")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .jwtID(UUID.randomUUID().toString())
            .claim("azp", clientId)
            .claim("scope", scope)
            .build();

        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
            claimsSet);
        jwt.sign(new RSASSASigner(JWK));
        return jwt.serialize();
    }

    private static KeyPair createKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("테스트 JWT 키를 생성할 수 없습니다.", exception);
        }
    }

    private static HttpServer startJwkServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/jwks", exchange -> {
                byte[] response = new JWKSet(JWK.toPublicJWK()).toString()
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream body = exchange.getResponseBody()) {
                    body.write(response);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("테스트 JWK 서버를 시작할 수 없습니다.", exception);
        }
    }

    @TestConfiguration
    static class TestEndpointConfiguration {

        @Bean
        AuthenticatedProbeController authenticatedProbeController() {
            return new AuthenticatedProbeController();
        }
    }

    @RestController
    static class AuthenticatedProbeController {

        @GetMapping("/test/authenticated")
        String authenticated() {
            return "authenticated";
        }

        @GetMapping("/api/accounts")
        String accounts() {
            return "accounts";
        }
    }
}
