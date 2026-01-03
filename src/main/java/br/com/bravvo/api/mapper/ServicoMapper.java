package br.com.bravvo.api.mapper;

import br.com.bravvo.api.dto.servico.ServicoCreateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoUpdateRequestDTO;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.StatusServico;

/**
 * Mapper responsável pela conversão entre: - DTOs de Serviço - Entidade Servico
 *
 * Segue o mesmo padrão do UserMapper: - métodos estáticos - sem estado - sem
 * lógica de negócio
 */
public class ServicoMapper {

	private ServicoMapper() {
		// evita instanciação
	}

	/**
	 * DTO (create) -> Entity
	 *
	 * Observações: - status é opcional no create; se vier nulo, assume ATIVO -
	 * createdAt / updatedAt são tratados pelos callbacks JPA
	 */
	public static Servico toEntity(ServicoCreateRequestDTO dto) {
		if (dto == null)
			return null;

		Servico servico = new Servico();
		servico.setNome(dto.getNome());
		servico.setDescricao(dto.getDescricao());
		servico.setDuracaoMin(dto.getDuracaoMin());
		servico.setValor(dto.getValor());

		// Se não vier status, assume ATIVO
		servico.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusServico.ATIVO);

		return servico;
	}

	/**
	 * Atualiza uma Entity existente com dados do DTO (update).
	 *
	 * Observações: - atualização completa do serviço - id, createdAt e updatedAt
	 * NÃO são alterados aqui
	 */
	public static void updateEntity(Servico servico, ServicoUpdateRequestDTO dto) {
		if (servico == null || dto == null)
			return;

		servico.setNome(dto.getNome());
		servico.setDescricao(dto.getDescricao());
		servico.setDuracaoMin(dto.getDuracaoMin());
		servico.setValor(dto.getValor());
		servico.setStatus(dto.getStatus());
	}

	/**
	 * Entity -> DTO (response)
	 *
	 * Utilizado em: - listagem - detalhe - create/update/status
	 */
	public static ServicoResponseDTO toResponse(Servico servico) {
		if (servico == null)
			return null;

		ServicoResponseDTO dto = new ServicoResponseDTO();
		dto.setId(servico.getId());
		dto.setNome(servico.getNome());
		dto.setDescricao(servico.getDescricao());
		dto.setDuracaoMin(servico.getDuracaoMin());
		dto.setValor(servico.getValor());
		dto.setStatus(servico.getStatus());
		dto.setCreatedAt(servico.getCreatedAt());
		dto.setUpdatedAt(servico.getUpdatedAt());

		return dto;
	}
}
