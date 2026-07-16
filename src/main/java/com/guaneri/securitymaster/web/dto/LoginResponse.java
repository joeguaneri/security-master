package com.guaneri.securitymaster.web.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
