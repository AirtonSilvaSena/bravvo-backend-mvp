package br.com.bravvo.api.dto.funcionario;

import java.time.LocalDateTime;

/**
 * Resposta do bloqueio.
 */
public class FuncionarioBloqueioResponseDTO {

	private Long id;
	private LocalDateTime startDt;
	private LocalDateTime endDt;
	private String motivo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getStartDt() {
		return startDt;
	}

	public void setStartDt(LocalDateTime startDt) {
		this.startDt = startDt;
	}

	public LocalDateTime getEndDt() {
		return endDt;
	}

	public void setEndDt(LocalDateTime endDt) {
		this.endDt = endDt;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
