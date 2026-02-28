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
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsável por ações do "módulo Funcionário" que são
 * auto-configuração (self-service).
 *
 * Admin NÃO usa isso.
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
     */
    public List<FuncionarioServicoConfigResponseDTO> getMeServicos() {

        // 1) Valida e recupera o funcionário logado via JWT (multi-tenant via vínculo)
        User funcionario = getFuncionarioLogado();

        // 2) Busca vínculos: quais serviços o funcionário habilitou
        Set<Long> habilitados = new HashSet<>(
                funcionarioServicoRepository.findServicoIdsByFuncionarioId(funcionario.getId())
        );

        // 3) Busca prefs: durações personalizadas por serviço
        Map<Long, Integer> duracoesCustom = loadDuracoesFromPrefs(funcionario.getId());

        // 4) Busca todos os serviços ativos do sistema
        var servicosAtivos = servicoRepository.findAllAtivos();

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
     * Importante: Admin NÃO acessa esse módulo por regra de produto.
     *
     * Multi-tenant (refatorado):
     * - Perfil NÃO é lido de User.
     * - Perfil é validado via vínculo EstabelecimentoUser (estabelecimento_users).
     */
    private User getFuncionarioLogado() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado."));

        if (!Boolean.TRUE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        EstabelecimentoUser vinculo = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
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
    private Map<Long, Integer> loadDuracoesFromPrefs(Long funcionarioId) {
        return funcionarioPrefsRepository.findById(funcionarioId)
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
     * Atualiza (sincroniza) os serviços habilitados e preferências do funcionário logado.
     *
     * Regras:
     * - Apenas FUNCIONARIO pode chamar
     * - Apenas serviços ATIVOS podem ser habilitados
     * - Persiste:
     *   - funcionario_servicos (vínculos)
     *   - funcionario_prefs.prefs_json (duração por serviço)
     */
    @Transactional
    public List<FuncionarioServicoConfigResponseDTO> updateMeServicos(FuncionarioServicosUpdateRequestDTO request) {

        // 1) valida e obtém funcionário logado
        User funcionario = getFuncionarioLogado();

        // 2) normaliza e valida request (evita NPE / ids duplicados)
        var items = request.getServicos();

        // Coleta IDs que o funcionário deseja habilitar
        List<Long> habilitarIds = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getHabilitado()))
                .map(FuncionarioServicoConfigItemRequestDTO::getServicoId)
                .distinct()
                .toList();

        // 3) valida se os serviços habilitados existem e estão ATIVOS
        if (!habilitarIds.isEmpty()) {
            List<Long> ativos = servicoRepository.findActiveIdsByIds(habilitarIds);
            Set<Long> ativosSet = new HashSet<>(ativos);

            List<Long> invalidos = habilitarIds.stream()
                    .filter(id -> !ativosSet.contains(id))
                    .toList();

            if (!invalidos.isEmpty()) {
                throw new BusinessException("Serviços inválidos ou inativos: " + invalidos);
            }
        }

        // 4) sincroniza funcionario_servicos (apaga tudo e recria)
        funcionarioServicoRepository.deleteAllByFuncionarioId(funcionario.getId());

        if (!habilitarIds.isEmpty()) {
            List<FuncionarioServico> novos = habilitarIds.stream().map(servicoId -> {
                FuncionarioServico fs = new FuncionarioServico();
                fs.setId(new FuncionarioServicoId(funcionario.getId(), servicoId));
                return fs;
            }).toList();

            funcionarioServicoRepository.saveAll(novos);
        }

        // 5) monta prefs_json com durações personalizadas
        Map<Long, Integer> duracoes = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getHabilitado()))
                .filter(i -> i.getDuracaoMin() != null)
                .collect(Collectors.toMap(
                        FuncionarioServicoConfigItemRequestDTO::getServicoId,
                        FuncionarioServicoConfigItemRequestDTO::getDuracaoMin,
                        (a, b) -> b
                ));

        String prefsJson = buildPrefsJson(duracoes);

        // 6) upsert em funcionario_prefs
        FuncionarioPrefs prefs = funcionarioPrefsRepository.findById(funcionario.getId()).orElseGet(() -> {
            FuncionarioPrefs p = new FuncionarioPrefs();
            p.setFuncionarioId(funcionario.getId());
            return p;
        });

        prefs.setPrefsJson(prefsJson);
        funcionarioPrefsRepository.save(prefs);

        // 7) retorna a lista atualizada
        return getMeServicos();
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