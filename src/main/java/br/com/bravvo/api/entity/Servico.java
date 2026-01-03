package br.com.bravvo.api.entity;

import br.com.bravvo.api.enums.StatusServico;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade Servico (tabela: servicos).
 *
 * Representa um item do catálogo de serviços.
 *
 * Observação importante: - O campo status é persistido como STRING (VARCHAR),
 * usando diretamente o valor do enum (ATIVO / INATIVO).
 */
@Entity
@Table(name = "servicos")
public class Servico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 500)
	private String descricao;

	/**
	 * Duração do serviço em minutos. Mapeia a coluna duracao_min do banco.
	 */
	@Column(name = "duracao_min", nullable = false)
	private Integer duracaoMin;

	/**
	 * Valor do serviço. DECIMAL(10,2) no banco.
	 */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	/**
	 * Status do serviço (ATIVO / INATIVO). Persistido como STRING no banco.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusServico status = StatusServico.ATIVO;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// =========================
	// Callbacks JPA
	// =========================

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();

		// Defesa: garante status padrão
		if (this.status == null) {
			this.status = StatusServico.ATIVO;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	// =========================
	// Getters and Setters
	// =========================

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Integer getDuracaoMin() {
		return duracaoMin;
	}

	public void setDuracaoMin(Integer duracaoMin) {
		this.duracaoMin = duracaoMin;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public StatusServico getStatus() {
		return status;
	}

	public void setStatus(StatusServico status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
