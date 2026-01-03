package br.com.bravvo.api.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * PK composta da tabela funcionario_servicos: (funcionario_id, servico_id)
 */
@Embeddable
public class FuncionarioServicoId implements Serializable {

	private Long funcionarioId;
	private Long servicoId;

	public FuncionarioServicoId() {
	}

	public FuncionarioServicoId(Long funcionarioId, Long servicoId) {
		this.funcionarioId = funcionarioId;
		this.servicoId = servicoId;
	}

	public Long getFuncionarioId() {
		return funcionarioId;
	}

	public void setFuncionarioId(Long funcionarioId) {
		this.funcionarioId = funcionarioId;
	}

	public Long getServicoId() {
		return servicoId;
	}

	public void setServicoId(Long servicoId) {
		this.servicoId = servicoId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FuncionarioServicoId that))
			return false;
		return Objects.equals(funcionarioId, that.funcionarioId) && Objects.equals(servicoId, that.servicoId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(funcionarioId, servicoId);
	}
}
