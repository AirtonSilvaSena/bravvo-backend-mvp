package br.com.bravvo.api.dto.agendamento;

import jakarta.validation.constraints.*;

/**
 * Request DTO para FUNCIONARIO criar agendamento.
 *
 * Cenários: A) Cliente cadastrado: clienteId preenchido B) Visitante:
 * clienteNome / clienteTelefone (email opcional)
 *
 * Endpoint: POST /api/funcionarios/agendamentos
 *
 * IMPORTANTE: - funcionarioId é SEMPRE derivado do JWT (não vem no body)
 */
public class FuncionarioAgendamentoCreateRequestDTO {

	@NotNull
	private Long servicoId;

	@NotBlank
	private String data; // yyyy-MM-dd

	@NotBlank
	private String hora; // HH:mm

	// ===== Cliente cadastrado =====
	private Long clienteId;

	// ===== Visitante =====
	@Size(max = 120)
	private String clienteNome;

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

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
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
