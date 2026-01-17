package br.com.bravvo.api.dto.estabelecimento;

import java.time.LocalDateTime;

public class EstabelecimentoMeResponseDTO {

	private Long id;
	private String nome;
	private String telefone;
	private String ramoAtuacao;
	private String endereco;
	private String numero;
	private String bairro;
	private String estado;
	private String cidade;
	private String slug;
	private String statusAssinatura;
	private LocalDateTime trialEndsAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public EstabelecimentoMeResponseDTO() {
	}

	public EstabelecimentoMeResponseDTO(Long id, String nome, String telefone, String ramoAtuacao, String endereco,
			String numero, String bairro, String estado, String cidade, String slug, String statusAssinatura,
			LocalDateTime trialEndsAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.nome = nome;
		this.telefone = telefone;
		this.ramoAtuacao = ramoAtuacao;
		this.endereco = endereco;
		this.numero = numero;
		this.bairro = bairro;
		this.estado = estado;
		this.cidade = cidade;
		this.slug = slug;
		this.statusAssinatura = statusAssinatura;
		this.trialEndsAt = trialEndsAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getTelefone() {
		return telefone;
	}

	public String getRamoAtuacao() {
		return ramoAtuacao;
	}

	public String getEndereco() {
		return endereco;
	}

	public String getNumero() {
		return numero;
	}

	public String getBairro() {
		return bairro;
	}

	public String getEstado() {
		return estado;
	}

	public String getCidade() {
		return cidade;
	}

	public String getSlug() {
		return slug;
	}

	public String getStatusAssinatura() {
		return statusAssinatura;
	}

	public LocalDateTime getTrialEndsAt() {
		return trialEndsAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public void setRamoAtuacao(String ramoAtuacao) {
		this.ramoAtuacao = ramoAtuacao;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public void setStatusAssinatura(String statusAssinatura) {
		this.statusAssinatura = statusAssinatura;
	}

	public void setTrialEndsAt(LocalDateTime trialEndsAt) {
		this.trialEndsAt = trialEndsAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
