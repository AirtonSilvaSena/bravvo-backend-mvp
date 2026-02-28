package br.com.bravvo.api.entity;

import br.com.bravvo.api.enums.PerfilUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidade de relacionamento (multi-tenant): Tabela: estabelecimento_users
 *
 * Representa o vínculo do usuário com um estabelecimento, incluindo o perfil
 * (ADMIN/FUNCIONARIO/CLIENTE) POR estabelecimento.
 */
@Entity
@Table(name = "estabelecimento_users", uniqueConstraints = {
		@UniqueConstraint(name = "uk_estabelecimento_user", columnNames = { "estabelecimento_id",
				"user_id" }) }, indexes = { @Index(name = "idx_eu_user", columnList = "user_id"),
						@Index(name = "idx_eu_estabelecimento", columnList = "estabelecimento_id"),
						@Index(name = "idx_eu_perfil_ativo", columnList = "perfil,ativo") })
public class EstabelecimentoUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "estabelecimento_id", nullable = false)
	private Long estabelecimentoId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "perfil", nullable = false, length = 50)
	private PerfilUser perfil;

	@Column(name = "ativo", nullable = false)
	private Boolean ativo = true;

	/**
	 * Mantido como read-only pois o banco já controla default/current_timestamp.
	 */
	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * Mantido como read-only pois o banco já controla on update current_timestamp.
	 */
	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	// getters / setters
	public Long getId() {
		return id;
	}

	public Long getEstabelecimentoId() {
		return estabelecimentoId;
	}

	public void setEstabelecimentoId(Long estabelecimentoId) {
		this.estabelecimentoId = estabelecimentoId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public PerfilUser getPerfil() {
		return perfil;
	}

	public void setPerfil(PerfilUser perfil) {
		this.perfil = perfil;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}