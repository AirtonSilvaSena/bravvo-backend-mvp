package br.com.bravvo.api.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * PK composta da tabela funcionario_agenda: (funcionario_id, dia_semana)
 *
 * diaSemana: 1=Segunda ... 7=Domingo
 */
@Embeddable
public class FuncionarioAgendaId implements Serializable {

	private Long funcionarioId;
	private Integer diaSemana;

	public FuncionarioAgendaId() {
	}

	public FuncionarioAgendaId(Long funcionarioId, Integer diaSemana) {
		this.funcionarioId = funcionarioId;
		this.diaSemana = diaSemana;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
	}

	public Integer getDiaSemana() {
		return diaSemana;
	}

	public void setDiaSemana(Integer diaSemana) {
		this.diaSemana = diaSemana;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FuncionarioAgendaId that))
			return false;
		return Objects.equals(funcionarioId, that.funcionarioId) && Objects.equals(diaSemana, that.diaSemana);
	}

	@Override
	public int hashCode() {
		return Objects.hash(funcionarioId, diaSemana);
	}
}
