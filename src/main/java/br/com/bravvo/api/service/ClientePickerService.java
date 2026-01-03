package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.cliente.ClientePickerResponseDTO;
import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de apoio ao fluxo do FUNCIONÁRIO: - listar/filtrar clientes
 * cadastrados para seleção no agendamento.
 *
 * Retorna no padrão do projeto: PagedResponseDTO { page, limit, total, pages,
 * items }
 */
@Service
public class ClientePickerService {

	private final UserRepository userRepository;

	public ClientePickerService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public PagedResponseDTO<ClientePickerResponseDTO> listClientes(int page, int limit, String q) {

		// =========================
		// Normalização defensiva
		// =========================
		int safePage = Math.max(page, 1); // API é 1-based
		int safeLimit = Math.min(Math.max(limit, 1), 50); // trava simples p/ não estourar

		Pageable pageable = PageRequest.of(safePage - 1, safeLimit, Sort.by("nome").ascending());

		String qNorm = (q == null || q.isBlank()) ? null : q.trim();

		// =========================
		// Busca: apenas CLIENTE e ativo=true
		// =========================
		var result = userRepository.searchAtivosByPerfilAndQ(PerfilUser.CLIENTE, qNorm, pageable);

		// =========================
		// Mapeia para DTO de resposta
		// =========================
		List<ClientePickerResponseDTO> items = result.getContent().stream()
				.map(u -> new ClientePickerResponseDTO(u.getId(), u.getNome(), u.getTelefone(), u.getEmail())).toList();

		// =========================
		// Monta DTO paginado do projeto
		// =========================
		return new PagedResponseDTO<>(safePage, safeLimit, result.getTotalElements(), result.getTotalPages(), items);
	}
}
