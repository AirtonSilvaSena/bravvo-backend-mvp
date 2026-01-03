package br.com.bravvo.api.dto.agendamento;

import java.time.LocalDateTime;

/**
 * Item de agendamento para listagens (MVP).
 *
 * Regras: - Sem dados sensíveis extras. - Serve para: cliente / funcionário /
 * público (com o que já existe na tabela).
 */
public class AgendamentoItemResponseDTO {

	private Long id;
	private String protocolo;

	private Long servicoId;
	private Long funcionarioId;

	private Long clienteId;
	private String clienteNome;
	private String clienteTelefone;
	private String clienteEmail;

	private LocalDateTime inicio;
	private LocalDateTime fim;

	private String status;
	private String observacoes;

	public AgendamentoItemResponseDTO() {
	}

	public AgendamentoItemResponseDTO(Long id, String protocolo, Long servicoId, Long funcionarioId, Long clienteId,
			String clienteNome, String clienteTelefone, String clienteEmail, LocalDateTime inicio, LocalDateTime fim,
			String status, String observacoes) {
		this.id = id;
		this.protocolo = protocolo;
		this.servicoId = servicoId;
		this.funcionarioId = funcionarioId;
		this.clienteId = clienteId;
		this.clienteNome = clienteNome;
		this.clienteTelefone = clienteTelefone;
		this.clienteEmail = clienteEmail;
		this.inicio = inicio;
		this.fim = fim;
		this.status = status;
		this.observacoes = observacoes;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProtocolo() {
		return protocolo;
	}

	public void setProtocolo(String protocolo) {
		this.protocolo = protocolo;
	}

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

	public LocalDateTime getInicio() {
		return inicio;
	}

	public void setInicio(LocalDateTime inicio) {
		this.inicio = inicio;
	}

	public LocalDateTime getFim() {
		return fim;
	}

	public void setFim(LocalDateTime fim) {
		this.fim = fim;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}
}
