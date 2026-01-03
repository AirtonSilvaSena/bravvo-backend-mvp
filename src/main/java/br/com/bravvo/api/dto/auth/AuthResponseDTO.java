package br.com.bravvo.api.dto.auth;

public class AuthResponseDTO {

	private String tokenType = "Bearer";
	private String accessToken;
	private String refreshToken;
	private Long expiresInSeconds;

	public AuthResponseDTO() {
	}

	public AuthResponseDTO(String accessToken, String refreshToken, Long expiresInSeconds) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresInSeconds = expiresInSeconds;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Long getExpiresInSeconds() {
		return expiresInSeconds;
	}

	public void setExpiresInSeconds(Long expiresInSeconds) {
		this.expiresInSeconds = expiresInSeconds;
	}
}
