package com.credito.common.security.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTokenClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void issuesClientCredentialsTokenWithRequestedScopes() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                {
                  "access_token": "service-token",
                  "token_type": "Bearer",
                  "expires_in": 300,
                  "scope": "accounts.read"
                }
                """);
        });
        server.start();

        ServiceTokenClient client = new ServiceTokenClient(
            URI.create("http://localhost:" + server.getAddress().getPort() + "/token"),
            "gateway-service",
            "secret");

        ServiceTokenClient.ServiceTokenResponse response = client.issueToken(List.of("accounts.read"));

        assertEquals("service-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(300L, response.expiresIn());
        assertEquals("accounts.read", response.scope());
        assertTrue(requestBody.get().contains("grant_type=client_credentials"));
        assertTrue(requestBody.get().contains("client_id=gateway-service"));
        assertTrue(requestBody.get().contains("scope=accounts.read"));
    }

    @Test
    void rejectsTokenResponseWithoutRequiredFields() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> respond(exchange, 200, """
            {
              "token_type": "Bearer",
              "expires_in": 300
            }
            """));
        server.start();

        ServiceTokenClient client = new ServiceTokenClient(
            URI.create("http://localhost:" + server.getAddress().getPort() + "/token"),
            "gateway-service",
            "secret");

        assertThrows(ServiceTokenException.class, () -> client.issueToken(List.of("accounts.read")));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(response);
        }
    }
}
