package br.com.bravvo.api.dto.publico;

import java.math.BigDecimal;

/**
 * DTO de resposta para listar funcionários por serviço no catálogo público.
 *
 * Endpoint: GET /api/public/servicos/{servicoId}/funcionarios
 *
 * Regras: - Retorna funcionários ATIVOS (perfil FUNCIONARIO) - Não expõe
 * email/telefone (somente nome) - Retorna valor do serviço - Retorna duração
 * RESOLVIDA por funcionário: - prefs_json -> servicos -> { "<servicoId>": {
 * "duracaoMin": X } } - fallback: Servico.duracaoMin
 */
public class PublicFuncionarioServicoResponseDTO {

	private Long funcionarioId;
	private String nome;
	private BigDecimal valor;
	private Integer duracaoMin;

	public PublicFuncionarioServicoResponseDTO() {
	}

	public PublicFuncionarioServicoResponseDTO(Long funcionarioId, String nome, BigDecimal valor, Integer duracaoMin) {
		this.funcionarioId = funcionarioId;
		this.nome = nome;
		this.valor = valor;
		this.duracaoMin = duracaoMin;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
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

	public Integer getDuracaoMin() {
		return duracaoMin;
	}

	public void setDuracaoMin(Integer duracaoMin) {
		this.duracaoMin = duracaoMin;
	}
}
