package br.com.bravvo.api.dto.agendamento;

import java.time.LocalDateTime;

/**
 * Response DTO após criação do agendamento.
 *
 * O front usa: - protocolo (principal) - inicio/fim para exibir resumo
 */
public class AgendamentoCreateResponseDTO {

	private Long id;
	private String protocolo;
	private LocalDateTime inicio;
	private LocalDateTime fim;
	private String status;

	public AgendamentoCreateResponseDTO() {
	}

	public AgendamentoCreateResponseDTO(Long id, String protocolo, LocalDateTime inicio, LocalDateTime fim,
			String status) {
		this.id = id;
		this.protocolo = protocolo;
		this.inicio = inicio;
		this.fim = fim;
		this.status = status;
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
}
