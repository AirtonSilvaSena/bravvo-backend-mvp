package br.com.bravvo.api.dto.publico;

import java.math.BigDecimal;

/**
 * DTO de resposta do catálogo público de serviços.
 *
 * Endpoint: GET /api/public/servicos
 *
 * Regras: - Retorna apenas serviços ATIVOS - Resposta mínima e segura para
 * visitante (sem login)
 */
public class PublicServicoResponseDTO {

	private Long id;
	private String nome;
	private BigDecimal valor;

	public PublicServicoResponseDTO() {
	}

	public PublicServicoResponseDTO(Long id, String nome, BigDecimal valor) {
		this.id = id;
		this.nome = nome;
		this.valor = valor;
	}

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

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}
}
