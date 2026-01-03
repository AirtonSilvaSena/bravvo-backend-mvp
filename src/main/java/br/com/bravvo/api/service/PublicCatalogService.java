package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.publico.PublicFuncionarioServicoResponseDTO;
import br.com.bravvo.api.dto.publico.PublicServicoResponseDTO;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.entity.Servico;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusServico;
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
 * Endpoints:
 * - GET /api/public/servicos
 * - GET /api/public/servicos/{servicoId}/funcionarios
 *
 * Segurança:
 * - Não expõe dados sensíveis do funcionário
 * - Defensivo contra prefs_json inválido (não pode quebrar a API)
 */
@Service
public class PublicCatalogService {

    private final ServicoRepository servicoRepository;
    private final FuncionarioServicoRepository funcionarioServicoRepository;
    private final FuncionarioPrefsRepository funcionarioPrefsRepository;
    private final ObjectMapper objectMapper;

    public PublicCatalogService(
            ServicoRepository servicoRepository,
            FuncionarioServicoRepository funcionarioServicoRepository,
            FuncionarioPrefsRepository funcionarioPrefsRepository,
            ObjectMapper objectMapper
    ) {
        this.servicoRepository = servicoRepository;
        this.funcionarioServicoRepository = funcionarioServicoRepository;
        this.funcionarioPrefsRepository = funcionarioPrefsRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lista serviços públicos:
     * - apenas ATIVOS
     * - resposta mínima (id, nome, valor)
     * - sem duração aqui (a duração correta é por funcionário)
     */
    public List<PublicServicoResponseDTO> listServicosPublicos() {
        List<Servico> ativos = servicoRepository.findAllAtivos();

        return ativos.stream()
                .map(s -> new PublicServicoResponseDTO(s.getId(), s.getNome(), s.getValor()))
                .collect(Collectors.toList());
    }

    /**
     * Lista funcionários que executam um serviço específico, retornando:
     * - nome do funcionário (sem dados sensíveis)
     * - valor do serviço
     * - duração resolvida por funcionário (prefs -> fallback serviço)
     */
    public List<PublicFuncionarioServicoResponseDTO> listFuncionariosPorServico(Long servicoId) {

        // =========================
        // 1) Valida serviço existente e ATIVO
        // =========================
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado."));

        if (servico.getStatus() != StatusServico.ATIVO) {
            // catálogo público não deve revelar status interno; "indisponível" é suficiente
            throw new NotFoundException("Serviço indisponível.");
        }

        // =========================
        // 2) Busca funcionários ativos que executam o serviço
        // =========================
        List<FuncionarioBasicProjection> funcionarios =
                funcionarioServicoRepository.findFuncionariosAtivosByServicoId(servicoId, PerfilUser.FUNCIONARIO);

        if (funcionarios.isEmpty()) {
            return Collections.emptyList();
        }

        // =========================
        // 3) Busca prefs em lote (evita N+1)
        // =========================
        List<Long> funcionarioIds = funcionarios.stream()
                .map(FuncionarioBasicProjection::getId)
                .toList();

        Map<Long, FuncionarioPrefs> prefsMap = funcionarioPrefsRepository.findAllById(funcionarioIds).stream()
                .collect(Collectors.toMap(FuncionarioPrefs::getFuncionarioId, p -> p));

        // =========================
        // 4) Monta resposta com duração resolvida
        // =========================
        List<PublicFuncionarioServicoResponseDTO> result = new ArrayList<>();

        for (FuncionarioBasicProjection f : funcionarios) {

            Integer duracaoResolvida = resolveDuracaoMin(
                    prefsMap.get(f.getId()),
                    servicoId,
                    servico.getDuracaoMin()
            );

            result.add(new PublicFuncionarioServicoResponseDTO(
                    f.getId(),
                    f.getNome(),
                    servico.getValor(),
                    duracaoResolvida
            ));
        }

        return result;
    }

    /**
     * Resolve duração do serviço para o funcionário:
     * - tenta ler do prefs_json no formato:
     *   {
     *     "servicos": {
     *       "1": { "duracaoMin": 30 }
     *     }
     *   }
     * - fallback para a duração padrão do serviço
     *
     * Defesa:
     * - se JSON estiver inválido, retorna fallback
     * - se duração estiver inválida (<1), retorna fallback
     */
    private Integer resolveDuracaoMin(FuncionarioPrefs prefs, Long servicoId, Integer fallbackDuracaoMin) {

        if (prefs == null || prefs.getPrefsJson() == null || prefs.getPrefsJson().isBlank()) {
            return fallbackDuracaoMin;
        }

        try {
            JsonNode root = objectMapper.readTree(prefs.getPrefsJson());

            JsonNode duracaoNode = root.path("servicos")
                    .path(String.valueOf(servicoId))
                    .path("duracaoMin");

            if (duracaoNode != null && duracaoNode.isInt()) {
                int v = duracaoNode.asInt();
                return v >= 1 ? v : fallbackDuracaoMin;
            }

            return fallbackDuracaoMin;
        } catch (Exception e) {
            // Não derrubar catálogo público por prefs_json inválido
            return fallbackDuracaoMin;
        }
    }
}
