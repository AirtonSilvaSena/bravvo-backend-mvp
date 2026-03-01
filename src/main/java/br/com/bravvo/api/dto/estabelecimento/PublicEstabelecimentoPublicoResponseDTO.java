package br.com.bravvo.api.dto.estabelecimento;

import java.time.LocalDateTime;

/**
 * DTO público para exibir informações do estabelecimento em páginas públicas
 * (ex.: agendamento). - Não inclui dados internos/sensíveis. - Pode ser
 * consumido sem autenticação.
 */
public class PublicEstabelecimentoPublicoResponseDTO {

	private String slug;
	private String nome;
	private String telefone;
	private String ramoAtuacao;

	private String endereco;
	private String numero;
	private String bairro;
	private String cidade;
	private String estado;

	private String statusAssinatura;
	private LocalDateTime trialEndsAt;

	private String logoUrl;

	public PublicEstabelecimentoPublicoResponseDTO() {
	}

	public PublicEstabelecimentoPublicoResponseDTO(String slug, String nome, String telefone, String ramoAtuacao,
			String endereco, String numero, String bairro, String cidade, String estado, String statusAssinatura,
			LocalDateTime trialEndsAt, String logoUrl) {
		this.slug = slug;
		this.nome = nome;
		this.telefone = telefone;
		this.ramoAtuacao = ramoAtuacao;
		this.endereco = endereco;
		this.numero = numero;
		this.bairro = bairro;
		this.cidade = cidade;
		this.estado = estado;
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

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getRamoAtuacao() {
		return ramoAtuacao;
	}

	public void setRamoAtuacao(String ramoAtuacao) {
		this.ramoAtuacao = ramoAtuacao;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getBairro() {
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
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