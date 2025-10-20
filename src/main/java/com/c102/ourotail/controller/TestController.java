package com.c102.ourotail.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for SDK
 * Used to verify that this controller works properly when this library is added as a dependency to other servers
 */
@Component
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * GET request test
     * @return simple response message
     */
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Ourotail SDK!");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * POST request test
     * @param request request data
     * @return response including request data
     */
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Echo from Ourotail SDK");
        response.put("receivedData", request);
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Get SDK information
     * @return SDK version and information
     */
    @GetMapping("/info")
    public Map<String, Object> getSdkInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("sdkName", "Ourotail SDK");
        response.put("version", "0.0.1-SNAPSHOT");
        response.put("description", "K6 SDK for Spring Boot");
        response.put("status", "active");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
