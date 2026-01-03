package br.com.bravvo.api.dto.agendamento;

import jakarta.validation.constraints.*;

/**
 * Request DTO para CLIENTE logado criar agendamento para si.
 *
 * Endpoint: POST /api/agendamentos
 *
 * Observação: - clienteId vem do JWT (não é enviado no body)
 */
public class ClienteAgendamentoCreateRequestDTO {

	@NotNull
	private Long servicoId;

	@NotNull
	private Long funcionarioId;

	@NotBlank
	private String data; // yyyy-MM-dd

	@NotBlank
	private String hora; // HH:mm

	@Size(max = 500)
	private String observacoes;

	public Long getServicoId() {
		return servicoId;
	}

	public void setServicoId(Long servicoId) {
		this.servicoId = servicoId;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getHora() {
		return hora;
	}

	public void setHora(String hora) {
		this.hora = hora;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}
}
