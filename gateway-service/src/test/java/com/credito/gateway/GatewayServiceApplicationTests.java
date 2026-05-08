package com.credito.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GatewayServiceApplicationTests {

    @Autowired
    private Environment environment;

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
}
