package br.com.bravvo.api.dto.servico;

import br.com.bravvo.api.enums.StatusServico;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para atualização SOMENTE do status do serviço.
 *
 * Endpoint: PUT /api/servicos/{id}/status
 *
 * Body: { "status": "ATIVO" | "INATIVO" }
 */
public class ServicoStatusUpdateRequestDTO {

	@NotNull(message = "Status é obrigatório")
	private StatusServico status;

	public StatusServico getStatus() {
		return status;
	}

	public void setStatus(StatusServico status) {
		this.status = status;
	}
}
