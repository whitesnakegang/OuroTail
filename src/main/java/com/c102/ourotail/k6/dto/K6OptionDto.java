package com.c102.ourotail.k6.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "K6 성능 테스트 옵션")
public class K6OptionDto {
    private String targetUrl;
    private int virtualUsers;
    private int duration;
    private String method;
    private String body;

    // Getters and Setters
    @Schema(description = "테스트 대상 URL", example = "http://localhost:8080/api/test/hello")
    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    @Schema(description = "가상 사용자 수", example = "10", minimum = "1", maximum = "1000")
    public int getVirtualUsers() {
        return virtualUsers;
    }

    public void setVirtualUsers(int virtualUsers) {
        this.virtualUsers = virtualUsers;
    }

    @Schema(description = "테스트 지속 시간 (초)", example = "30", minimum = "1", maximum = "3600")
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Schema(description = "HTTP 메서드", example = "GET", allowableValues = {"GET", "POST", "PUT", "DELETE"})
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Schema(description = "요청 본문 (POST/PUT 메서드용)", example = "{\"test\": \"data\"}")
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
