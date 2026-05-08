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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "MANAGEMENT_SERVER_PORT=0",
        "MANAGEMENT_USERNAME=test-actuator",
        "MANAGEMENT_PASSWORD=test-secret"
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
        assertEquals("customer-service-route",
            environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].id"));
        assertEquals("account-service-route",
            environment.getProperty("spring.cloud.gateway.server.webmvc.routes[1].id"));
        assertEquals("lending-service-route",
            environment.getProperty("spring.cloud.gateway.server.webmvc.routes[2].id"));
        assertEquals("batch-service-route",
            environment.getProperty("spring.cloud.gateway.server.webmvc.routes[3].id"));
    }

    @Test
    void keepsActuatorOffThePublicPort() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + applicationPort + "/actuator/gateway/routes",
            String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void securesGatewayActuatorEndpointOnManagementPort() {
        ResponseEntity<String> unauthorizedResponse = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/gateway/routes",
            String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, unauthorizedResponse.getStatusCode());

        ResponseEntity<String> authorizedResponse = restTemplate
            .withBasicAuth(managementUsername, managementPassword)
            .getForEntity("http://localhost:" + managementPort + "/actuator/gateway/routes", String.class);

        assertEquals(HttpStatus.OK, authorizedResponse.getStatusCode());
        assertTrue(authorizedResponse.getBody() != null && authorizedResponse.getBody().contains("route_id"));
    }

    @Test
    void keepsHealthEndpointPublicOnManagementPort() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + managementPort + "/actuator/health",
            String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
