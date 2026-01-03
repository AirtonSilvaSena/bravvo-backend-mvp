package br.com.bravvo.api.dto.agendamento;

import jakarta.validation.constraints.*;

/**
 * Request DTO para visitante (sem login) criar agendamento.
 *
 * Endpoint: POST /api/public/agendamentos
 */
public class PublicAgendamentoCreateRequestDTO {

	@NotNull
	private Long servicoId;

	@NotNull
	private Long funcionarioId;

	/**
	 * Data escolhida no front (yyyy-MM-dd).
	 */
	@NotBlank
	private String data;

	/**
	 * Hora escolhida no front (HH:mm).
	 */
	@NotBlank
	private String hora;

	@NotBlank
	@Size(max = 120)
	private String clienteNome;

	@NotBlank
	@Size(max = 30)
	private String clienteTelefone;

	@Email
	@Size(max = 180)
	private String clienteEmail;

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

	public String getClienteNome() {
		return clienteNome;
	}

	public void setClienteNome(String clienteNome) {
		this.clienteNome = clienteNome;
	}

	public String getClienteTelefone() {
		return clienteTelefone;
	}

	public void setClienteTelefone(String clienteTelefone) {
		this.clienteTelefone = clienteTelefone;
	}

	public String getClienteEmail() {
		return clienteEmail;
	}

	public void setClienteEmail(String clienteEmail) {
		this.clienteEmail = clienteEmail;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}
}
