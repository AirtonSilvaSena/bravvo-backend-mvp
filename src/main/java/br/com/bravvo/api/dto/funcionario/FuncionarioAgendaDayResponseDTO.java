package br.com.bravvo.api.dto.funcionario;

import java.time.LocalTime;

/**
 * Resposta de um dia da agenda semanal.
 *
 * Sempre retornamos os 7 dias. Se ainda não configurado, vem ativo=false.
 */
public class FuncionarioAgendaDayResponseDTO {

	private Integer diaSemana;
	private Boolean ativo;

	private LocalTime inicio1;
	private LocalTime fim1;

	private LocalTime inicio2;
	private LocalTime fim2;

	public Integer getDiaSemana() {
		return diaSemana;
	}

	public void setDiaSemana(Integer diaSemana) {
		this.diaSemana = diaSemana;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public LocalTime getInicio1() {
		return inicio1;
	}

	public void setInicio1(LocalTime inicio1) {
		this.inicio1 = inicio1;
	}

	public LocalTime getFim1() {
		return fim1;
	}

	public void setFim1(LocalTime fim1) {
		this.fim1 = fim1;
	}

	public LocalTime getInicio2() {
		return inicio2;
	}

	public void setInicio2(LocalTime inicio2) {
		this.inicio2 = inicio2;
	}

	public LocalTime getFim2() {
		return fim2;
	}

	public void setFim2(LocalTime fim2) {
		this.fim2 = fim2;
	}
}
