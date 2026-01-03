package br.com.bravvo.api.dto.funcionario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Criação de bloqueio (dia inteiro ou intervalo).
 */
public class FuncionarioBloqueioCreateRequestDTO {

	@NotNull(message = "startDt é obrigatório")
	private LocalDateTime startDt;

	@NotNull(message = "endDt é obrigatório")
	private LocalDateTime endDt;

	@Size(max = 255, message = "motivo deve ter no máximo 255 caracteres")
	private String motivo;

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
