package br.com.bravvo.api.dto.funcionario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Representa um serviço na tela do funcionário (REQUEST).
 *
 * - servicoId: ID do serviço - habilitado: se o funcionário quer trabalhar com
 * esse serviço - duracaoMin: tempo personalizado (opcional, mas se vier deve
 * ser válido)
 */
public class FuncionarioServicoConfigItemRequestDTO {

	@NotNull(message = "servicoId é obrigatório")
	private Long servicoId;

	@NotNull(message = "habilitado é obrigatório")
	private Boolean habilitado;

	/**
	 * Duração opcional. Se vier preenchida, deve ser >= 1. Se não vier, o sistema
	 * pode usar a duração padrão do serviço no agendamento.
	 */
	@Min(value = 1, message = "duracaoMin deve ser no mínimo 1 minuto")
	private Integer duracaoMin;

	public Long getServicoId() {
		return servicoId;
	}

	public void setServicoId(Long servicoId) {
		this.servicoId = servicoId;
	}

	public Boolean getHabilitado() {
		return habilitado;
	}

	public void setHabilitado(Boolean habilitado) {
		this.habilitado = habilitado;
	}

	public Integer getDuracaoMin() {
		return duracaoMin;
	}

	public void setDuracaoMin(Integer duracaoMin) {
		this.duracaoMin = duracaoMin;
	}
}
