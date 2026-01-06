package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "salao_pre_cadastros")
public class SalaoPreCadastro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "email", nullable = false, length = 180, unique = true)
	private String email;

	@Column(name = "telefone", length = 30)
	private String telefone;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Column(name = "slug", nullable = false, length = 60, unique = true)
	private String slug;

	@Column(name = "codigo_hash", nullable = false, length = 255)
	private String codigoHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "attempts", nullable = false)
	private Integer attempts = 0;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	// getters/setters
	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getCodigoHash() {
		return codigoHash;
	}

	public void setCodigoHash(String codigoHash) {
		this.codigoHash = codigoHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
