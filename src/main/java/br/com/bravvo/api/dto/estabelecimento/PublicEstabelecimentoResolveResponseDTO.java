package br.com.bravvo.api.dto.estabelecimento;

import java.time.LocalDateTime;

public class PublicEstabelecimentoResolveResponseDTO {
	private String slug;
	private String nome;
	private String statusAssinatura;
	private LocalDateTime trialEndsAt;
	private String logoUrl;

	public PublicEstabelecimentoResolveResponseDTO() {
	}

	public PublicEstabelecimentoResolveResponseDTO(String slug, String nome, String statusAssinatura,
			LocalDateTime trialEndsAt, String logoUrl) {
		this.slug = slug;
		this.nome = nome;
		this.statusAssinatura = statusAssinatura;
		this.trialEndsAt = trialEndsAt;
		this.logoUrl = logoUrl;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getStatusAssinatura() {
		return statusAssinatura;
	}

	public void setStatusAssinatura(String statusAssinatura) {
		this.statusAssinatura = statusAssinatura;
	}

	public LocalDateTime getTrialEndsAt() {
		return trialEndsAt;
	}

	public void setTrialEndsAt(LocalDateTime trialEndsAt) {
		this.trialEndsAt = trialEndsAt;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}
}