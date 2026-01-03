package br.com.bravvo.api.mapper;

import br.com.bravvo.api.dto.agendamento.AgendamentoItemResponseDTO;
import br.com.bravvo.api.entity.Agendamento;

/**
 * Mapper simples (MVP) para Agendamento -> DTO.
 */
public class AgendamentoMapper {

    private AgendamentoMapper() {}

    public static AgendamentoItemResponseDTO toItemDTO(Agendamento a) {
        return new AgendamentoItemResponseDTO(
                a.getId(),
                a.getProtocolo(),
                a.getServicoId(),
                a.getFuncionarioId(),
                a.getClienteId(),
                a.getClienteNome(),
                a.getClienteTelefone(),
                a.getClienteEmail(),
                a.getInicio(),
                a.getFim(),
                a.getStatus(),
                a.getObservacoes()
        );
    }
}
