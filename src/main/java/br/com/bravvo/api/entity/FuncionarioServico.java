package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa o vínculo: Funcionário (User) ↔ Serviço.
 *
 * Tabela: funcionario_servicos Colunas: funcionario_id, servico_id, created_at
 */
@Entity
@Table(name = "funcionario_servicos")
public class FuncionarioServico {

	@EmbeddedId
	private FuncionarioServicoId id;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public FuncionarioServicoId getId() {
		return id;
	}

	public void setId(FuncionarioServicoId id) {
		this.id = id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
