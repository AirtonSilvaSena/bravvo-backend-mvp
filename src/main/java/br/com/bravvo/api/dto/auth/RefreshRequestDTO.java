package br.com.bravvo.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDTO {

    @NotBlank
    private String refreshToken;

    /**
     * Necessário em multi-tenant real, pois o refresh token sozinho não define qual tenant emitir novo JWT.
     */
    @NotBlank
    private String slug;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}