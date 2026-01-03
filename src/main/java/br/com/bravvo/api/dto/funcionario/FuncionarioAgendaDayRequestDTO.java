package br.com.bravvo.api.dto.funcionario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Um dia da agenda semanal do funcionário.
 *
 * diaSemana: 1=Seg ... 7=Dom
 *
 * Regras: - Se ativo=false, horários podem vir null - Se ativo=true: - inicio1
 * e fim1 são obrigatórios e inicio1 < fim1 - janela2 é opcional, mas se vier
 * deve ter inicio2+fim2 e inicio2 < fim2 - janela2 não pode sobrepor janela1:
 * fim1 <= inicio2
 */
public class FuncionarioAgendaDayRequestDTO {

	@NotNull(message = "diaSemana é obrigatório")
	@Min(value = 1, message = "diaSemana deve ser 1..7")
	@Max(value = 7, message = "diaSemana deve ser 1..7")
	private Integer diaSemana;

	private Boolean ativo = true;

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
