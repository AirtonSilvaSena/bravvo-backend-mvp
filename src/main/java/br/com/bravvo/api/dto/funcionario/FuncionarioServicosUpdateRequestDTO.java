package br.com.bravvo.api.dto.funcionario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * REQUEST do PUT /api/funcionarios/me/servicos
 *
 * Recebe a configuração completa da tela (lista de serviços com habilitado +
 * duração).
 */
public class FuncionarioServicosUpdateRequestDTO {

	@NotNull(message = "servicos é obrigatório")
	@Valid
	private List<FuncionarioServicoConfigItemRequestDTO> servicos;

	public List<FuncionarioServicoConfigItemRequestDTO> getServicos() {
		return servicos;
	}

	public void setServicos(List<FuncionarioServicoConfigItemRequestDTO> servicos) {
		this.servicos = servicos;
	}
}
