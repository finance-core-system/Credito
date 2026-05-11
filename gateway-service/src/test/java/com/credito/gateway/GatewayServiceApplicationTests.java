package com.credito.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	    properties = {
	        "SERVER_PORT=0",
	        "CUSTOMER_SERVICE_PORT=8081",
	        "ACCOUNT_SERVICE_PORT=8082",
	        "LENDING_SERVICE_PORT=8083",
	        "BATCH_SERVICE_PORT=8084",
	        "MANAGEMENT_SERVER_PORT=0",
	        "MANAGEMENT_USERNAME=test-actuator",
	        "MANAGEMENT_PASSWORD=test-secret",
	        "CUSTOMER_REALM_ISSUER_URI=http://localhost:8085/realms/customer-realm",
	        "CUSTOMER_REALM_JWK_SET_URI=http://localhost:8085/realms/customer-realm/protocol/openid-connect/certs",
	        "CUSTOMER_REALM_INTERNAL_ISSUER_URI=http://keycloak:8080/realms/customer-realm",
	        "CUSTOMER_REALM_INTERNAL_JWK_SET_URI=http://localhost:8085/realms/customer-realm/protocol/openid-connect/certs",
	        "ADMIN_REALM_ISSUER_URI=http://localhost:8085/realms/admin-realm",
	        "ADMIN_REALM_JWK_SET_URI=http://localhost:8085/realms/admin-realm/protocol/openid-connect/certs",
	        "ADMIN_REALM_INTERNAL_ISSUER_URI=http://keycloak:8080/realms/admin-realm",
	        "ADMIN_REALM_INTERNAL_JWK_SET_URI=http://localhost:8085/realms/admin-realm/protocol/openid-connect/certs"
	    }
	)
class GatewayServiceApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int applicationPort;

    @LocalManagementPort
    private int managementPort;

    @Value("${spring.security.user.name}")
    private String managementUsername;

    @Value("${spring.security.user.password}")
    private String managementPassword;

    @Test
    void contextLoads() {
    }

    @Test
    void configuresGatewayRoutesForAllBusinessServices() {
        Set<String> routeIds = IntStream.range(0, 4)
            .mapToObj(index -> environment.getProperty("spring.cloud.gateway.server.webmvc.routes[" + index + "].id"))
            .collect(Collectors.toSet());

        assertEquals(Set.of(
            "customer-service-route",
            "account-service-route",
            "lending-service-route",
            "batch-service-route"
        ), routeIds);
    }

    @Test
    void keepsActuatorOffThePublicPort() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + applicationPort + "/actuator/gateway/routes",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void securesActuatorLinksOnManagementPort() {
        ResponseEntity<String> unauthorizedResponse = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, unauthorizedResponse.getStatusCode());

        ResponseEntity<String> authorizedResponse = restTemplate
            .withBasicAuth(managementUsername, managementPassword)
            .getForEntity("http://localhost:" + managementPort + "/actuator", String.class);

        assertEquals(HttpStatus.OK, authorizedResponse.getStatusCode());
        assertTrue(authorizedResponse.getBody() != null && authorizedResponse.getBody().contains("health"));
    }

    @Test
    void keepsHealthEndpointPublicOnManagementPort() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/health",
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void applicationRoutesRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + applicationPort + "/api/customers",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
