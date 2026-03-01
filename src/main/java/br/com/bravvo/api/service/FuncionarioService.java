package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.funcionario.FuncionarioServicoConfigItemRequestDTO;
import br.com.bravvo.api.dto.funcionario.FuncionarioServicoConfigResponseDTO;
import br.com.bravvo.api.dto.funcionario.FuncionarioServicosUpdateRequestDTO;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.FuncionarioPrefs;
import br.com.bravvo.api.entity.FuncionarioServico;
import br.com.bravvo.api.entity.FuncionarioServicoId;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.FuncionarioPrefsRepository;
import br.com.bravvo.api.repository.FuncionarioServicoRepository;
import br.com.bravvo.api.repository.ServicoRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsável por ações do "módulo Funcionário" que são
 * auto-configuração (self-service).
 *
 * Admin NÃO usa isso.
 *
 * Multi-tenant:
 * - Sempre filtrar por estabelecimentoId (TenantContext).
 * - Identidade do usuário vem do token (userId), sem depender de e-mail global.
 * - Perfil vem do vínculo estabelecimento_users (EstabelecimentoUser), não do User.
 */
@Service
public class FuncionarioService {

    private final UserRepository userRepository;
    private final ServicoRepository servicoRepository;
    private final FuncionarioServicoRepository funcionarioServicoRepository;
    private final FuncionarioPrefsRepository funcionarioPrefsRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FuncionarioService(
            UserRepository userRepository,
            ServicoRepository servicoRepository,
            FuncionarioServicoRepository funcionarioServicoRepository,
            FuncionarioPrefsRepository funcionarioPrefsRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository
    ) {
        this.userRepository = userRepository;
        this.servicoRepository = servicoRepository;
        this.funcionarioServicoRepository = funcionarioServicoRepository;
        this.funcionarioPrefsRepository = funcionarioPrefsRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
    }

    /**
     * Retorna a lista de serviços (ativos) configuráveis pelo funcionário logado,
     * com:
     * - habilitado (funcionario_servicos)
     * - duracaoFuncionarioMin (funcionario_prefs)
     *
     * Multi-tenant: tudo filtrado por estabelecimentoId.
     */
    public List<FuncionarioServicoConfigResponseDTO> getMeServicos() {

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

        // 1) Valida e recupera o funcionário logado via JWT (multi-tenant via vínculo)
        User funcionario = getFuncionarioLogado(estabelecimentoId);

        // 2) Busca vínculos: quais serviços o funcionário habilitou (TENANT-SAFE)
        Set<Long> habilitados = new HashSet<>(
                funcionarioServicoRepository.findServicoIdsByEstabelecimentoIdAndFuncionarioId(
                        estabelecimentoId,
                        funcionario.getId()
                )
        );

        // 3) Busca prefs: durações personalizadas por serviço (TENANT-SAFE)
        Map<Long, Integer> duracoesCustom = loadDuracoesFromPrefs(estabelecimentoId, funcionario.getId());

        // 4) Busca todos os serviços ativos do estabelecimento (TENANT-SAFE)
        var servicosAtivos = servicoRepository.findAllAtivosByEstabelecimentoId(estabelecimentoId);

        // 5) Monta o formato final para o front
        return servicosAtivos.stream().map(servico -> {
            var dto = new FuncionarioServicoConfigResponseDTO();
            dto.setId(servico.getId());
            dto.setNome(servico.getNome());
            dto.setDescricao(servico.getDescricao());
            dto.setValor(servico.getValor());

            dto.setDuracaoPadraoMin(servico.getDuracaoMin());

            boolean isHabilitado = habilitados.contains(servico.getId());
            dto.setHabilitado(isHabilitado);

            // Duração personalizada (se existir), senão usa padrão
            Integer duracao = duracoesCustom.get(servico.getId());
            dto.setDuracaoFuncionarioMin(duracao != null ? duracao : servico.getDuracaoMin());

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Atualiza (sincroniza) os serviços habilitados e preferências do funcionário logado.
     *
     * Regras:
     * - Apenas FUNCIONARIO pode chamar
     * - Apenas serviços ATIVOS do estabelecimento podem ser habilitados
     * - Persiste:
     *   - funcionario_servicos (vínculos)
     *   - funcionario_prefs.prefs_json (duração por serviço)
     *
     * Multi-tenant: tudo filtrado por estabelecimentoId.
     */
    @Transactional
    public List<FuncionarioServicoConfigResponseDTO> updateMeServicos(FuncionarioServicosUpdateRequestDTO request) {

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

        // 1) valida e obtém funcionário logado
        User funcionario = getFuncionarioLogado(estabelecimentoId);

        // 2) normaliza request
        var items = (request != null && request.getServicos() != null)
                ? request.getServicos()
                : Collections.<FuncionarioServicoConfigItemRequestDTO>emptyList();

        // Coleta IDs que o funcionário deseja habilitar
        List<Long> habilitarIds = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getHabilitado()))
                .map(FuncionarioServicoConfigItemRequestDTO::getServicoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3) valida se os serviços habilitados existem e estão ATIVOS (TENANT-SAFE)
        if (!habilitarIds.isEmpty()) {
            List<Long> ativos = servicoRepository.findActiveIdsByEstabelecimentoIdAndIds(estabelecimentoId, habilitarIds);
            Set<Long> ativosSet = new HashSet<>(ativos);

            List<Long> invalidos = habilitarIds.stream()
                    .filter(id -> !ativosSet.contains(id))
                    .toList();

            if (!invalidos.isEmpty()) {
                throw new BusinessException("Serviços inválidos ou inativos: " + invalidos);
            }
        }

        // 4) sincroniza funcionario_servicos (TENANT-SAFE): apaga tudo do funcionário no tenant e recria
        funcionarioServicoRepository.deleteAllByEstabelecimentoIdAndFuncionarioId(estabelecimentoId, funcionario.getId());

        if (!habilitarIds.isEmpty()) {
            List<FuncionarioServico> novos = habilitarIds.stream().map(servicoId -> {
                FuncionarioServico fs = new FuncionarioServico();
                // Se sua entidade FuncionarioServico tem coluna estabelecimentoId:
                fs.setEstabelecimentoId(estabelecimentoId);

                // ID composto atual (funcionarioId, servicoId)
                fs.setId(new FuncionarioServicoId(funcionario.getId(), servicoId));
                return fs;
            }).toList();

            funcionarioServicoRepository.saveAll(novos);
        }

        // 5) monta prefs_json com durações personalizadas
        Map<Long, Integer> duracoes = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getHabilitado()))
                .filter(i -> i.getServicoId() != null)
                .filter(i -> i.getDuracaoMin() != null)
                .collect(Collectors.toMap(
                        FuncionarioServicoConfigItemRequestDTO::getServicoId,
                        FuncionarioServicoConfigItemRequestDTO::getDuracaoMin,
                        (a, b) -> b
                ));

        String prefsJson = buildPrefsJson(duracoes);

        // 6) upsert em funcionario_prefs (TENANT-SAFE)
        FuncionarioPrefs prefs = funcionarioPrefsRepository
                .findByEstabelecimentoIdAndFuncionarioId(estabelecimentoId, funcionario.getId())
                .orElseGet(() -> {
                    FuncionarioPrefs p = new FuncionarioPrefs();
                    p.setEstabelecimentoId(estabelecimentoId);
                    p.setFuncionarioId(funcionario.getId());
                    return p;
                });

        prefs.setPrefsJson(prefsJson);
        funcionarioPrefsRepository.save(prefs);

        // 7) retorna a lista atualizada
        return getMeServicos();
    }

    // ==========================================================
    // Auxiliares
    // ==========================================================

    /**
     * Recupera o usuário logado e garante que:
     * - está autenticado
     * - existe no banco
     * - está ativo
     * - vínculo no tenant atual está ativo
     * - perfil do vínculo é FUNCIONARIO
     *
     * Multi-tenant:
     * - userId vem do token (TenantContext).
     * - vínculo validado por (estabelecimentoId, userId).
     */
    private User getFuncionarioLogado(Long estabelecimentoId) {

        Long userId = TenantContext.getUserIdOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));

        if (!Boolean.TRUE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        EstabelecimentoUser vinculo = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, userId)
                .orElseThrow(() -> new ForbiddenException("Vínculo do usuário com o estabelecimento não encontrado."));

        if (Boolean.FALSE.equals(vinculo.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        if (vinculo.getPerfil() != PerfilUser.FUNCIONARIO) {
            throw new ForbiddenException("Acesso permitido apenas para funcionários.");
        }

        return user;
    }

    /**
     * Carrega o JSON de prefs e converte em: servicoId -> duracaoMin
     *
     * Se não existir prefs, retorna vazio.
     */
    private Map<Long, Integer> loadDuracoesFromPrefs(Long estabelecimentoId, Long funcionarioId) {
        return funcionarioPrefsRepository
                .findByEstabelecimentoIdAndFuncionarioId(estabelecimentoId, funcionarioId)
                .map(prefs -> parseDuracoes(prefs.getPrefsJson()))
                .orElse(Collections.emptyMap());
    }

    /**
     * Parse seguro do JSON (não pode quebrar tela).
     *
     * Formato esperado: { "servicos": { "1": { "duracaoMin": 30 } } }
     */
    private Map<Long, Integer> parseDuracoes(String prefsJson) {

        if (prefsJson == null || prefsJson.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            JsonNode root = objectMapper.readTree(prefsJson);
            JsonNode servicosNode = root.get("servicos");

            if (servicosNode == null || !servicosNode.isObject()) {
                return Collections.emptyMap();
            }

            Map<Long, Integer> result = new HashMap<>();

            Iterator<String> it = servicosNode.fieldNames();
            while (it.hasNext()) {
                String servicoIdStr = it.next();
                JsonNode item = servicosNode.get(servicoIdStr);

                JsonNode duracaoNode = (item != null) ? item.get("duracaoMin") : null;
                if (duracaoNode != null && duracaoNode.isInt()) {
                    result.put(Long.valueOf(servicoIdStr), duracaoNode.asInt());
                }
            }

            return result;

        } catch (Exception e) {
            // Importante: se JSON estiver inválido, não quebra nada.
            return Collections.emptyMap();
        }
    }

    /**
     * Monta o JSON no formato padrão definido:
     * { "servicos": { "1": { "duracaoMin": 30 }, "2": { "duracaoMin": 45 } } }
     */
    private String buildPrefsJson(Map<Long, Integer> duracoes) {
        try {
            ObjectNode root = JsonNodeFactory.instance.objectNode();
            ObjectNode servicosNode = root.putObject("servicos");

            for (var entry : duracoes.entrySet()) {
                String servicoId = String.valueOf(entry.getKey());
                int duracaoMin = entry.getValue();

                ObjectNode item = servicosNode.putObject(servicoId);
                item.put("duracaoMin", duracaoMin);
            }

            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new BusinessException("Erro ao salvar preferências do funcionário.");
        }
    }
}