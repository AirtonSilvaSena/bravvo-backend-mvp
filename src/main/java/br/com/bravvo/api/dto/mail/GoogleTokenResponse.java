package br.com.bravvo.api.dto.mail;

public record GoogleTokenResponse(
        String access_token,
        Integer expires_in,
        String token_type
) {}
