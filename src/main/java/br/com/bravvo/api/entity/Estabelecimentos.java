package br.com.bravvo.api.entity;

import br.com.bravvo.api.enums.StatusAssinatura;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "estabelecimentos")
public class Estabelecimentos {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;
	
	@Column(name = "telefone", nullable = true, length = 100)
	private String telefone;
	
	@Column(name = "ramo_atuacao", length = 60)
	private String ramoAtuacao;

	@Column(name = "endereco", nullable = true, length = 255)
	private String endereco;
	
	@Column(name = "numero", nullable = true, length = 20)
	private String numero;
	
	@Column(name = "bairro", nullable = true, length = 100)
	private String bairro;
	
	@Column(name = "estado", nullable = true, length = 100)
	private String estado;
	
	@Column(name = "cidade", nullable = true, length = 100)
	private String cidade;
	
	
	@Column(name = "slug", nullable = false, length = 60, unique = true)
	private String slug;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_assinatura", nullable = false, length = 20)
	private StatusAssinatura statusAssinatura = StatusAssinatura.TRIAL;

	@Column(name = "trial_ends_at")
	private LocalDateTime trialEndsAt;

	// Dono do salão (admin)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_user_id")
	private User ownerUser;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

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
	
	public String getRamoAtuacao() {
		return ramoAtuacao;
	}

	public void setRamoAtuacao(String ramoAtuacao) {
		this.ramoAtuacao = ramoAtuacao;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public StatusAssinatura getStatusAssinatura() {
		return statusAssinatura;
	}

	public void setStatusAssinatura(StatusAssinatura statusAssinatura) {
		this.statusAssinatura = statusAssinatura;
	}

	public LocalDateTime getTrialEndsAt() {
		return trialEndsAt;
	}

	public void setTrialEndsAt(LocalDateTime trialEndsAt) {
		this.trialEndsAt = trialEndsAt;
	}

	public User getOwnerUser() {
		return ownerUser;
	}

	public void setOwnerUser(User ownerUser) {
		this.ownerUser = ownerUser;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	
	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
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

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

}
