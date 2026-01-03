package br.com.bravvo.api.dto.funcionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Atualização completa da agenda semanal.
 *
 * MVP: sempre enviar os 7 dias (1..7).
 */
public class FuncionarioAgendaUpdateRequestDTO {

	@NotEmpty(message = "agenda é obrigatória")
	private List<@Valid FuncionarioAgendaDayRequestDTO> agenda;

	public List<FuncionarioAgendaDayRequestDTO> getAgenda() {
		return agenda;
	}

	public void setAgenda(List<FuncionarioAgendaDayRequestDTO> agenda) {
		this.agenda = agenda;
	}
}
