package br.com.bravvo.api.entity;

import br.com.bravvo.api.enums.StatusAssinatura;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity Estabelecimentos - alinhada com o schema atual do banco.
 *
 * Tabela: estabelecimentos
 *
 * Regras do schema: - slug é UNIQUE (uk_estabelecimentos_slug) -
 * status_assinatura possui CHECK: TRIAL | ATIVO | INADIMPLENTE | CANCELADO -
 * created_at/updated_at possuem defaults no banco (current_timestamp / on
 * update) - owner_user_id referencia users(id)
 */
@Entity
@Table(name = "estabelecimentos", uniqueConstraints = {
		@UniqueConstraint(name = "uk_estabelecimentos_slug", columnNames = { "slug" }) }, indexes = {
				@Index(name = "idx_estabelecimentos_status", columnList = "status_assinatura"),
				@Index(name = "idx_estabelecimentos_owner", columnList = "owner_user_id") })
public class Estabelecimentos {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	/**
	 * BD: varchar(20) DEFAULT NULL
	 */
	@Column(name = "telefone", length = 20)
	private String telefone;

	/**
	 * BD: varchar(60) NOT NULL
	 */
	@Column(name = "ramo_atuacao", nullable = false, length = 60)
	private String ramoAtuacao;

	@Column(name = "endereco", length = 255)
	private String endereco;

	@Column(name = "numero", length = 20)
	private String numero;

	@Column(name = "bairro", length = 100)
	private String bairro;

	@Column(name = "estado", length = 100)
	private String estado;

	@Column(name = "cidade", length = 100)
	private String cidade;

	@Column(name = "slug", nullable = false, length = 60)
	private String slug;
	
	/**
	 * CEP do estabelecimento.
	 * BD: varchar(9) NULL
	 */
	@Column(name = "cep", length = 9)
	private String cep;
	
	/**
	 * Texto institucional público do estabelecimento.
	 * BD: TEXT NULL
	 */
	@Column(name = "sobre_nos", columnDefinition = "TEXT")
	private String sobreNos;

	/**
	 * URL pública do Instagram.
	 * BD: varchar(255) NULL
	 */
	@Column(name = "instagram_url", length = 255)
	private String instagramUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_assinatura", nullable = false, length = 20)
	private StatusAssinatura statusAssinatura = StatusAssinatura.TRIAL;

	@Column(name = "trial_ends_at")
	private LocalDateTime trialEndsAt;

	/**
	 * BD: owner_user_id bigint unsigned DEFAULT NULL FK:
	 * fk_estabelecimentos_owner_user -> users(id)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_user_id", foreignKey = @ForeignKey(name = "fk_estabelecimentos_owner_user"))
	private User ownerUser;

	/**
	 * BD controla via DEFAULT current_timestamp()
	 */
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * BD controla via DEFAULT current_timestamp() ON UPDATE current_timestamp()
	 */
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "logo_key", length = 255)
	private String logoKey;

	@Column(name = "logo_mime_type", length = 100)
	private String logoMimeType;

	@Column(name = "logo_size_bytes")
	private Long logoSizeBytes;

	@Column(name = "logo_updated_at")
	private LocalDateTime logoUpdatedAt;

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

	public String getLogoKey() {
		return logoKey;
	}

	public void setLogoKey(String logoKey) {
		this.logoKey = logoKey;
	}

	public String getLogoMimeType() {
		return logoMimeType;
	}

	public void setLogoMimeType(String logoMimeType) {
		this.logoMimeType = logoMimeType;
	}

	public Long getLogoSizeBytes() {
		return logoSizeBytes;
	}

	public void setLogoSizeBytes(Long logoSizeBytes) {
		this.logoSizeBytes = logoSizeBytes;
	}

	public LocalDateTime getLogoUpdatedAt() {
		return logoUpdatedAt;
	}

	public void setLogoUpdatedAt(LocalDateTime logoUpdatedAt) {
		this.logoUpdatedAt = logoUpdatedAt;
	}
	
	public String getSobreNos() {
		return sobreNos;
	}

	public void setSobreNos(String sobreNos) {
		this.sobreNos = sobreNos;
	}

	public String getInstagramUrl() {
		return instagramUrl;
	}

	public void setInstagramUrl(String instagramUrl) {
		this.instagramUrl = instagramUrl;
	}
	
	public String getCep() {
		return cep;
	}
	
	public void setCep(String cep) {
		this.cep = cep;
	}
}