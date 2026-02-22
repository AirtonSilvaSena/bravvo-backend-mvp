package br.com.bravvo.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import br.com.bravvo.api.dto.mail.GoogleTokenResponse;

import java.time.Instant;

@Service
public class GmailTokenService {

    @Value("${gmail.client-id}")
    private String clientId;

    @Value("${gmail.client-secret}")
    private String clientSecret;

    @Value("${gmail.refresh-token}")
    private String refreshToken;

    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedAccessToken;
    private Instant accessTokenExpiresAt;

    public synchronized String getAccessToken() {
        // se ainda é válido (com folga de 60s)
        if (cachedAccessToken != null && accessTokenExpiresAt != null) {
            if (Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
                return cachedAccessToken;
            }
        }

        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<GoogleTokenResponse> resp =
                restTemplate.exchange(url, HttpMethod.POST, entity, GoogleTokenResponse.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().access_token() == null) {
            throw new RuntimeException("Falha ao obter access_token via refresh_token (Gmail API).");
        }

        cachedAccessToken = resp.getBody().access_token();
        int expiresIn = resp.getBody().expires_in() != null ? resp.getBody().expires_in() : 3600;
        accessTokenExpiresAt = Instant.now().plusSeconds(expiresIn);

        return cachedAccessToken;
    }
}