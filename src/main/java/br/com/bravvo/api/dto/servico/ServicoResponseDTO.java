package br.com.bravvo.api.dto.servico;

import br.com.bravvo.api.enums.StatusServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta do Serviço.
 *
 * Usado em: - listagem (GET /api/servicos) - detalhes (GET /api/servicos/{id})
 * - create/update/status
 */
public class ServicoResponseDTO {

	private Long id;
	private String nome;
	private String descricao;
	private Integer duracaoMin;
	private BigDecimal valor;
	private StatusServico status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
