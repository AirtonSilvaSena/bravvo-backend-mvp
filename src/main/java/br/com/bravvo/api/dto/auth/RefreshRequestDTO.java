package br.com.bravvo.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequestDTO {

	@NotBlank(message = "Refresh token é obrigatório.")
	private String refreshToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
