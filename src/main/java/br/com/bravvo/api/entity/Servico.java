package br.com.bravvo.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Servico - alinhada com o schema atual do banco.
 *
 * Tabela: servicos
 *
 * Observações do schema: - estabelecimento_id é DEFAULT NULL (nullable). -
 * status é varchar(20) NOT NULL DEFAULT 'ativo' (string no banco). - created_at
 * e updated_at possuem defaults e ON UPDATE (controlados pelo banco).
 */
@Entity
@Table(name = "servicos", indexes = { @Index(name = "idx_servicos_status", columnList = "status"),
		@Index(name = "idx_servicos_nome", columnList = "nome"),
		@Index(name = "idx_servicos_estabelecimento", columnList = "estabelecimento_id"),
		@Index(name = "idx_servicos_estabelecimento_status", columnList = "estabelecimento_id,status") })
public class Servico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * BD: bigint unsigned DEFAULT NULL
	 */
	@Column(name = "estabelecimento_id")
	private Long estabelecimentoId;

	@Column(name = "nome", nullable = false, length = 120)
	private String nome;

	@Column(name = "descricao", length = 500)
	private String descricao;

	@Column(name = "duracao_min", nullable = false)
	private Integer duracaoMin;

	/**
	 * BD: decimal(10,2) NOT NULL DEFAULT 0.00
	 */
	@Column(name = "valor", nullable = false, precision = 10, scale = 2)
	private BigDecimal valor = BigDecimal.ZERO;

	/**
	 * BD: varchar(20) NOT NULL DEFAULT 'ativo'
	 *
	 * Mantemos como String para refletir 1:1 o schema atual. (Quando quiser,
	 * migramos para enum + converter.)
	 */
	@Column(name = "status", nullable = false, length = 20)
	private String status = "ativo";

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

	// getters/setters

	public Long getId() {
		return id;
	}

	public Long getEstabelecimentoId() {
		return estabelecimentoId;
	}

	public void setEstabelecimentoId(Long estabelecimentoId) {
		this.estabelecimentoId = estabelecimentoId;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}