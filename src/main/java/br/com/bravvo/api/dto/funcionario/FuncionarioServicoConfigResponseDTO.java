package br.com.bravvo.api.dto.funcionario;

import java.math.BigDecimal;

/**
 * DTO para alimentar a tela de "Serviços" do FUNCIONÁRIO.
 *
 * Retorna: - dados do serviço (nome/valor/duração padrão) - se está habilitado
 * (funcionario_servicos) - duração personalizada (funcionario_prefs.prefs_json)
 */
public class FuncionarioServicoConfigResponseDTO {

	private Long id;
	private String nome;
	private String descricao;
	private BigDecimal valor;

	private Integer duracaoPadraoMin;

	private Boolean habilitado;

	private Integer duracaoFuncionarioMin;

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

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public Integer getDuracaoPadraoMin() {
		return duracaoPadraoMin;
	}

	public void setDuracaoPadraoMin(Integer duracaoPadraoMin) {
		this.duracaoPadraoMin = duracaoPadraoMin;
	}

	public Boolean getHabilitado() {
		return habilitado;
	}

	public void setHabilitado(Boolean habilitado) {
		this.habilitado = habilitado;
	}

	public Integer getDuracaoFuncionarioMin() {
		return duracaoFuncionarioMin;
	}

	public void setDuracaoFuncionarioMin(Integer duracaoFuncionarioMin) {
		this.duracaoFuncionarioMin = duracaoFuncionarioMin;
	}
}
