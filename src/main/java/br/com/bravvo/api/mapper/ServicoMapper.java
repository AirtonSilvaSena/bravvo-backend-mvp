package br.com.bravvo.api.mapper;

import br.com.bravvo.api.dto.servico.ServicoCreateRequestDTO;
import br.com.bravvo.api.dto.servico.ServicoResponseDTO;
import br.com.bravvo.api.dto.servico.ServicoUpdateRequestDTO;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.StatusServico;

/**
 * Mapper responsável pela conversão entre: - DTOs de Serviço - Entidade Servico
 *
 * Observação importante (schema atual): - No BANCO, status é varchar(20) com
 * valores minúsculos: "ativo" / "inativo" - Na ENTITY (alinhada ao BD), status
 * é String - Nos DTOs, status permanece como enum StatusServico
 *
 * Portanto, este mapper converte: StatusServico <-> String (db)
 */
public class ServicoMapper {

	private ServicoMapper() {
		// evita instanciação
	}

	/**
	 * DTO (create) -> Entity
	 *
	 * Observações: - status é opcional no create; se vier null, assume ATIVO
	 * ("ativo") - createdAt/updatedAt são gerenciados pelo banco (na entity estão
	 * insertable/updatable=false)
	 */
	public static Servico toEntity(ServicoCreateRequestDTO dto) {
		if (dto == null)
			return null;

		Servico servico = new Servico();
		servico.setNome(dto.getNome());
		servico.setDescricao(dto.getDescricao());
		servico.setDuracaoMin(dto.getDuracaoMin());
		servico.setValor(dto.getValor());

		// ✅ converte enum -> string do banco
		servico.setStatus(toDbStatus(dto.getStatus()));

		return servico;
	}

	/**
	 * Atualiza Entity a partir do DTO (update)
	 *
	 * Observações: - Mantém o padrão do seu projeto: update completo. - status no
	 * DTO pode vir null; se vier null, mantemos o status atual (não sobrescreve).
	 */
	public static void updateEntity(Servico servico, ServicoUpdateRequestDTO dto) {
		if (servico == null || dto == null)
			return;

		servico.setNome(dto.getNome());
		servico.setDescricao(dto.getDescricao());
		servico.setDuracaoMin(dto.getDuracaoMin());
		servico.setValor(dto.getValor());

		// ✅ se vier status no dto, converte e atualiza; se não, mantém o atual
		if (dto.getStatus() != null) {
			servico.setStatus(toDbStatus(dto.getStatus()));
		}
	}

	/**
	 * Entity -> DTO (response)
	 *
	 * Observação: - converte string do banco -> enum
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

		// ✅ converte string do banco -> enum
		dto.setStatus(fromDbStatus(servico.getStatus()));

		dto.setCreatedAt(servico.getCreatedAt());
		dto.setUpdatedAt(servico.getUpdatedAt());

		return dto;
	}

	// ==========================================================
	// Conversões StatusServico <-> String (DB)
	// ==========================================================

	/**
	 * Converte o enum para o valor salvo no banco (minúsculo).
	 */
	private static String toDbStatus(StatusServico status) {
		if (status == null)
			return "ativo"; // default do BD

		// Ajuste aqui conforme seu enum real
		// Ex.: ATIVO/INATIVO
		return switch (status) {
		case ATIVO -> "ativo";
		case INATIVO -> "inativo";
		};
	}

	/**
	 * Converte o valor do banco (String) para o enum do sistema. Se vier algo
	 * inesperado, faz fallback para ATIVO.
	 */
	private static StatusServico fromDbStatus(String dbStatus) {
		if (dbStatus == null || dbStatus.isBlank())
			return StatusServico.ATIVO;

		String s = dbStatus.trim().toLowerCase();
		return switch (s) {
		case "ativo" -> StatusServico.ATIVO;
		case "inativo" -> StatusServico.INATIVO;
		default -> StatusServico.ATIVO;
		};
	}
}