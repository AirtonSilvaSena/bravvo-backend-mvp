package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.user.UserCreateRequestDTO;
import br.com.bravvo.api.dto.user.UserResponseDTO;
import br.com.bravvo.api.dto.user.UserUpdateRequestDTO;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.UserMapper;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO dto) {

        // ===============================
        // 1) Identifica o usuário logado
        // ===============================
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String emailLogado = TenantContext.getEmailOrThrow();

        // Perfil agora vem do vínculo no tenant
        EstabelecimentoUser callerLink = getLinkByTenantAndEmailOrThrow(estabelecimentoId, emailLogado);

        boolean isAdmin = callerLink.getPerfil() == PerfilUser.ADMIN;
        boolean isFuncionario = callerLink.getPerfil() == PerfilUser.FUNCIONARIO;

        // CLIENTE não pode criar usuário
        if (!isAdmin && !isFuncionario) {
            throw new ForbiddenException("Sem permissão para criar usuários.");
        }

        // ==================================
        // 2) Regras de criação por perfil
        // ==================================

        // ADMIN nunca pode ser criado via API
        if (dto.getPerfil() == PerfilUser.ADMIN) {
            throw new ForbiddenException("Criação de ADMIN não permitida via API.");
        }

        // FUNCIONARIO só pode criar CLIENTE
        if (isFuncionario && dto.getPerfil() != PerfilUser.CLIENTE) {
            throw new ForbiddenException("Funcionário só pode criar usuários com perfil CLIENTE.");
        }

        // ADMIN pode criar CLIENTE ou FUNCIONARIO
        if (isAdmin &&
                dto.getPerfil() != PerfilUser.CLIENTE &&
                dto.getPerfil() != PerfilUser.FUNCIONARIO) {

            throw new ForbiddenException("Perfil inválido para criação de usuário.");
        }

        // ===============================
        // 3) Fluxo atual (ajuste multi-tenant)
        // ===============================
        // Agora duplicidade é por tenant:
        // - user é global, então:
        //   a) pega/ cria user por email
        //   b) valida se já existe vínculo com esse estabelecimento
        String emailNovo = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase();
        if (emailNovo.isBlank()) throw new BusinessException("E-mail é obrigatório.");

        User user = userRepository.findByEmail(emailNovo).orElse(null);

        if (user != null) {
            // user existe globalmente -> checa se já tem vínculo nesse tenant
            boolean jaVinculado = estabelecimentoUserRepository.existsByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId());
            if (jaVinculado) {
                throw new BusinessException("Já existe um usuário com este e-mail neste estabelecimento.");
            }
        } else {
            // cria user global
            user = UserMapper.toEntity(dto);
            user.setEmail(emailNovo);

            String hash = passwordEncoder.encode(dto.getSenha());
            user.setSenhaHash(hash);
            user = userRepository.save(user);
        }

        // cria vínculo no tenant com o perfil do DTO
        EstabelecimentoUser link = new EstabelecimentoUser();
        link.setEstabelecimentoId(estabelecimentoId);
        link.setUserId(user.getId());
        link.setPerfil(dto.getPerfil());
        link.setAtivo(true);
        estabelecimentoUserRepository.save(link);

        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long id) {
        // mantém comportamento anterior (busca user global)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        return UserMapper.toResponse(user);
    }

    /**
     * Mantive o listAll como você tinha.
     * OBS: com multi-tenant, isso lista users globais e pode não ser desejado.
     * Estou mantendo porque você pediu ajustar só getperfil/search.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> listAll() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponseDTO update(Long id, UserUpdateRequestDTO dto) {

        // ===============================
        // 1) Identifica o usuário logado (perfil via vínculo)
        // ===============================
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String emailLogado = TenantContext.getEmailOrThrow();

        EstabelecimentoUser callerLink = getLinkByTenantAndEmailOrThrow(estabelecimentoId, emailLogado);

        boolean isAdmin = callerLink.getPerfil() == PerfilUser.ADMIN;
        boolean isFuncionario = callerLink.getPerfil() == PerfilUser.FUNCIONARIO;

        // CLIENTE não pode atualizar usuário
        if (!isAdmin && !isFuncionario) {
            throw new ForbiddenException("Sem permissão para atualizar usuários.");
        }

        // ===============================
        // 2) Busca o alvo e aplica regra (perfil via vínculo do alvo)
        // ===============================
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        EstabelecimentoUser targetLink = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                .orElseThrow(() -> new ForbiddenException("Usuário não pertence a este estabelecimento."));

        // FUNCIONARIO só pode atualizar CLIENTE
        if (isFuncionario && targetLink.getPerfil() != PerfilUser.CLIENTE) {
            throw new ForbiddenException("Funcionário só pode atualizar usuários com perfil CLIENTE.");
        }

        // ===============================
        // 3) Regras extras (perfil)
        // ===============================
        if (dto.getPerfil() != null && dto.getPerfil() == PerfilUser.ADMIN) {
            throw new ForbiddenException("Não é permitido definir perfil ADMIN via API.");
        }

        if (isFuncionario && dto.getPerfil() != null && dto.getPerfil() != PerfilUser.CLIENTE) {
            throw new ForbiddenException("Funcionário não pode alterar perfil do usuário para outro diferente de CLIENTE.");
        }

        // ===============================
        // 4) Fluxo atual (inalterado)
        // ===============================

        // se mudou email, valida duplicidade global + vínculo
        String emailAtual = user.getEmail();
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(emailAtual)) {
            String novoEmail = dto.getEmail().trim().toLowerCase();

            User existing = userRepository.findByEmail(novoEmail).orElse(null);
            if (existing != null && !existing.getId().equals(user.getId())) {
                boolean vinculadoNoTenant = estabelecimentoUserRepository
                        .existsByEstabelecimentoIdAndUserId(estabelecimentoId, existing.getId());
                if (vinculadoNoTenant) {
                    throw new BusinessException("Já existe um usuário com este e-mail neste estabelecimento.");
                }
            }

            user.setEmail(novoEmail);
        }

        UserMapper.updateEntity(user, dto);

        // se veio senha, atualiza hash
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        // se veio perfil, atualiza no vínculo do tenant
        if (dto.getPerfil() != null) {
            targetLink.setPerfil(dto.getPerfil());
            estabelecimentoUserRepository.save(targetLink);
        }

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void inactivate(Long id) {
        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndUserId(estabelecimentoId, user.getId())
                .orElseThrow(() -> new ForbiddenException("Usuário não pertence a este estabelecimento."));

        // Inativa no vínculo do tenant (mais correto no multi-tenant)
        link.setAtivo(false);
        estabelecimentoUserRepository.save(link);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<UserResponseDTO> listPaged(
            Integer page,
            Integer limit,
            PerfilUser perfil,
            Boolean ativo,
            String q
    ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Sem permissão para listar usuários.");
        }

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String emailLogado = TenantContext.getEmailOrThrow();

        // Perfil agora via vínculo
        EstabelecimentoUser callerLink = getLinkByTenantAndEmailOrThrow(estabelecimentoId, emailLogado);

        boolean isAdmin = callerLink.getPerfil() == PerfilUser.ADMIN;
        boolean isFuncionario = callerLink.getPerfil() == PerfilUser.FUNCIONARIO;

        if (!isAdmin && !isFuncionario) {
            throw new ForbiddenException("Sem permissão para listar usuários.");
        }

        // Regra: FUNCIONARIO só pode listar CLIENTE
        if (isFuncionario) {
            if (perfil != null && perfil != PerfilUser.CLIENTE) {
                throw new ForbiddenException("Funcionário só pode listar clientes.");
            }
            perfil = PerfilUser.CLIENTE;
        }

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeLimit = (limit == null || limit < 1) ? 10 : Math.min(limit, 100);

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // ✅ Aqui muda: search agora é pelo vínculo (tenant) + join no User
        Page<User> result = estabelecimentoUserRepository.searchUsersByTenant(
                estabelecimentoId,
                perfil,
                ativo,
                q,
                pageable
        );

        int pages = result.getTotalPages();
        long total = result.getTotalElements();

        var items = result.getContent().stream()
                .map(UserMapper::toResponse)
                .toList();

        return new PagedResponseDTO<>(
                safePage,
                safeLimit,
                total,
                pages,
                items
        );
    }

    // ==========================================================
    // Helpers
    // ==========================================================

    /**
     * Busca vínculo do caller no tenant atual a partir do email do JWT.
     * Implementação: precisa ser um @Query com JOIN em User (porque vínculo não tem email).
     */
    private EstabelecimentoUser getLinkByTenantAndEmailOrThrow(Long estabelecimentoId, String email) {
        EstabelecimentoUser link = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado neste estabelecimento."));

        if (Boolean.FALSE.equals(link.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        return link;
    }
}