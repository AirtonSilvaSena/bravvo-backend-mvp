package br.com.bravvo.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private String issuer;
	private String secret;
	private Integer accessTokenMinutes;
	private Integer refreshTokenDays;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Integer getAccessTokenMinutes() {
		return accessTokenMinutes;
	}

	public void setAccessTokenMinutes(Integer accessTokenMinutes) {
		this.accessTokenMinutes = accessTokenMinutes;
	}

	public Integer getRefreshTokenDays() {
		return refreshTokenDays;
	}

	public void setRefreshTokenDays(Integer refreshTokenDays) {
		this.refreshTokenDays = refreshTokenDays;
	}
}
