package com.credito.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "SERVER_PORT=0",
        "CUSTOMER_REALM_ISSUER_URI=http://localhost:8085/realms/customer-realm",
        "CUSTOMER_REALM_JWK_SET_URI=http://localhost:8085/realms/customer-realm/protocol/openid-connect/certs",
        "CUSTOMER_REALM_INTERNAL_ISSUER_URI=http://keycloak:8080/realms/customer-realm",
        "CUSTOMER_REALM_INTERNAL_JWK_SET_URI=http://localhost:8085/realms/customer-realm/protocol/openid-connect/certs",
        "ADMIN_REALM_ISSUER_URI=http://localhost:8085/realms/admin-realm",
        "ADMIN_REALM_JWK_SET_URI=http://localhost:8085/realms/admin-realm/protocol/openid-connect/certs",
        "ADMIN_REALM_INTERNAL_ISSUER_URI=http://keycloak:8080/realms/admin-realm",
        "ADMIN_REALM_INTERNAL_JWK_SET_URI=http://localhost:8085/realms/admin-realm/protocol/openid-connect/certs"
    })
class BatchServiceApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

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
            "http://localhost:" + port + "/api/batch",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
