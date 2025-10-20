package com.c102.ourotail.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = {OurotailAutoConfigurationTest.TestConfig.class, OurotailAutoConfiguration.class})
class OurotailAutoConfigurationTest {

    @Configuration
    static class TestConfig {
        // Test configuration
    }

    @Test
    void contextLoads() {
        // This test verifies that the auto-configuration loads successfully
    }
}
