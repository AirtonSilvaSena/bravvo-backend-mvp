package br.com.bravvo.api.dto.publico;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de resposta para disponibilidade pública por data.
 *
 * Endpoint: GET
 * /api/public/disponibilidade?servicoId=...&funcionarioId=...&data=yyyy-MM-dd
 *
 * Front: - usuário escolhe a data - backend devolve a lista de horários
 * disponíveis
 */
@Schema(description = "Disponibilidade pública por data (horários livres)")
public class PublicDisponibilidadeResponseDTO {

	@Schema(example = "2026-01-05", description = "Data consultada")
	private LocalDate data;

	@Schema(example = "60", description = "Duração resolvida do serviço (prefs do funcionário ou fallback do serviço)")
	private Integer duracaoMin;

	@Schema(example = "[\"09:00\",\"10:00\",\"11:00\",\"14:00\"]", description = "Horários disponíveis (HH:mm)")
	private List<String> horarios;

	public PublicDisponibilidadeResponseDTO() {
	}

	public PublicDisponibilidadeResponseDTO(LocalDate data, Integer duracaoMin, List<String> horarios) {
		this.data = data;
		this.duracaoMin = duracaoMin;
		this.horarios = horarios;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public Integer getDuracaoMin() {
		return duracaoMin;
	}

	public void setDuracaoMin(Integer duracaoMin) {
		this.duracaoMin = duracaoMin;
	}

	public List<String> getHorarios() {
		return horarios;
	}

	public void setHorarios(List<String> horarios) {
		this.horarios = horarios;
	}
}
