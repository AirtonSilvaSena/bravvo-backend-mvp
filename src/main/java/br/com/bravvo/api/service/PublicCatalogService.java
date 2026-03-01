package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.publico.PublicFuncionarioServicoResponseDTO;
import br.com.bravvo.api.dto.publico.PublicServicoResponseDTO;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.FuncionarioPrefsRepository;
import br.com.bravvo.api.repository.FuncionarioServicoRepository;
import br.com.bravvo.api.repository.ServicoRepository;
import br.com.bravvo.api.repository.projection.FuncionarioBasicProjection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsável pelo catálogo público (/api/public/**).
 *
 * Multi-tenant: - Todas as consultas são filtradas por estabelecimentoId
 * (resolvido por slug no controller).
 *
 * Segurança: - Não expõe dados sensíveis do funcionário. - Defensivo contra
 * prefs_json inválido (não derruba a API).
 */
@Service
public class PublicCatalogService {

	private final ServicoRepository servicoRepository;
	private final FuncionarioServicoRepository funcionarioServicoRepository;
	private final FuncionarioPrefsRepository funcionarioPrefsRepository;
	private final ObjectMapper objectMapper;

	public PublicCatalogService(ServicoRepository servicoRepository,
			FuncionarioServicoRepository funcionarioServicoRepository,
			FuncionarioPrefsRepository funcionarioPrefsRepository, ObjectMapper objectMapper) {
		this.servicoRepository = servicoRepository;
		this.funcionarioServicoRepository = funcionarioServicoRepository;
		this.funcionarioPrefsRepository = funcionarioPrefsRepository;
		this.objectMapper = objectMapper;
	}

	/**
	 * Lista serviços públicos por estabelecimento: - apenas ATIVOS - resposta
	 * mínima (id, nome, valor)
	 */
	public List<PublicServicoResponseDTO> listServicosPublicos(Long estabelecimentoId) {

		// OBS: seu repo precisa filtrar por estabelecimentoId e status="ativo"
		List<Servico> ativos = servicoRepository.findAllAtivosByEstabelecimentoId(estabelecimentoId);

		return ativos.stream().map(s -> new PublicServicoResponseDTO(s.getId(), s.getNome(), s.getValor()))
				.collect(Collectors.toList());
	}

	/**
	 * Lista funcionários que executam um serviço específico no tenant.
	 *
	 * Regras: - serviço deve existir e estar ATIVO no mesmo estabelecimento -
	 * funcionário deve: - estar ATIVO (users.ativo) - ter vínculo ATIVO no tenant
	 * com perfil FUNCIONARIO - ter o serviço habilitado (funcionario_servicos)
	 */
	public List<PublicFuncionarioServicoResponseDTO> listFuncionariosPorServico(Long estabelecimentoId,
			Long servicoId) {

		// 1) valida serviço do tenant
		Servico servico = servicoRepository.findByIdAndEstabelecimentoId(servicoId, estabelecimentoId)
				.orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

		// ✅ CORREÇÃO: status agora é String ("ativo"/"inativo")
		if (!isServicoAtivo(servico.getStatus())) {
			throw new NotFoundException("Serviço indisponível.");
		}

		// 2) busca funcionários aptos (JOIN vínculo + user ativo + perfil)
		List<FuncionarioBasicProjection> funcionarios = funcionarioServicoRepository
				.findFuncionariosAtivosByServicoId(estabelecimentoId, servicoId, PerfilUser.FUNCIONARIO);

		if (funcionarios.isEmpty()) {
			return Collections.emptyList();
		}

		// 3) prefs em lote (tenant-safe)
		List<Long> funcionarioIds = funcionarios.stream().map(FuncionarioBasicProjection::getId).toList();

		Map<Long, FuncionarioPrefs> prefsMap = funcionarioPrefsRepository
				.findAllByEstabelecimentoIdAndFuncionarioIdIn(estabelecimentoId, funcionarioIds).stream()
				.collect(Collectors.toMap(FuncionarioPrefs::getFuncionarioId, p -> p));

		// 4) resposta com duração resolvida
		List<PublicFuncionarioServicoResponseDTO> result = new ArrayList<>();

		for (FuncionarioBasicProjection f : funcionarios) {

			Integer duracaoResolvida = resolveDuracaoMin(prefsMap.get(f.getId()), servicoId, servico.getDuracaoMin());

			result.add(new PublicFuncionarioServicoResponseDTO(f.getId(), f.getNome(), servico.getValor(),
					duracaoResolvida));
		}

		return result;
	}

	/**
	 * Status do serviço no schema atual: - "ativo" / "inativo" (varchar)
	 */
	private boolean isServicoAtivo(String statusDb) {
		if (statusDb == null)
			return false;
		return "ativo".equalsIgnoreCase(statusDb.trim());
	}

	private Integer resolveDuracaoMin(FuncionarioPrefs prefs, Long servicoId, Integer fallbackDuracaoMin) {

		if (prefs == null || prefs.getPrefsJson() == null || prefs.getPrefsJson().isBlank()) {
			return fallbackDuracaoMin;
		}

		try {
			JsonNode root = objectMapper.readTree(prefs.getPrefsJson());

			JsonNode duracaoNode = root.path("servicos").path(String.valueOf(servicoId)).path("duracaoMin");

			if (duracaoNode != null && duracaoNode.isInt()) {
				int v = duracaoNode.asInt();
				return v >= 1 ? v : fallbackDuracaoMin;
			}

			return fallbackDuracaoMin;
		} catch (Exception e) {
			return fallbackDuracaoMin;
		}
	}
}