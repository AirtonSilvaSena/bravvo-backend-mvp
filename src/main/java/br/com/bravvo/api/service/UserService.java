package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.common.PagedResponseDTO;
import br.com.bravvo.api.dto.user.UserCreateRequestDTO;
import br.com.bravvo.api.dto.user.UserResponseDTO;
import br.com.bravvo.api.dto.user.UserUpdateRequestDTO;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.mapper.UserMapper;
import br.com.bravvo.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;   // <<< ESTE é o Pageable correto
import org.springframework.data.domain.Sort;       // <<< faltava
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isFuncionario = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO"));

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
        // 3) Fluxo atual (inalterado)
        // ===============================
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }

        User user = UserMapper.toEntity(dto);

        String hash = passwordEncoder.encode(dto.getSenha());
        user.setSenhaHash(hash);

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        return UserMapper.toResponse(user);
    }

    /**
     * Mantive o listAll como você tinha.
     * OBS: se o Controller trocar para paginação, este método pode deixar de ser usado.
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
        // 1) Identifica o usuário logado
        // ===============================
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isFuncionario = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FUNCIONARIO"));

        // CLIENTE não pode atualizar usuário
        if (!isAdmin && !isFuncionario) {
            throw new ForbiddenException("Sem permissão para atualizar usuários.");
        }

        // ===============================
        // 2) Busca o alvo e aplica regra
        // ===============================
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        // FUNCIONARIO só pode atualizar CLIENTE
        if (isFuncionario && user.getPerfil() != PerfilUser.CLIENTE) {
            throw new ForbiddenException("Funcionário só pode atualizar usuários com perfil CLIENTE.");
        }

        // ===============================
        // 3) Regras extras (perfil)
        // ===============================
        // Se o seu UserUpdateRequestDTO tiver "perfil", bloqueia mudança para ADMIN via API
        if (dto.getPerfil() != null && dto.getPerfil() == PerfilUser.ADMIN) {
            throw new ForbiddenException("Não é permitido definir perfil ADMIN via API.");
        }

        // (opcional recomendado) Se FUNCIONARIO tentar mudar perfil do CLIENTE para FUNCIONARIO, bloqueia
        if (isFuncionario && dto.getPerfil() != null && dto.getPerfil() != PerfilUser.CLIENTE) {
            throw new ForbiddenException("Funcionário não pode alterar perfil do usuário para outro diferente de CLIENTE.");
        }

        // ===============================
        // 4) Fluxo atual (inalterado)
        // ===============================

        // se mudou email, valida duplicidade
        String emailAtual = user.getEmail();
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(emailAtual)) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new BusinessException("Já existe um usuário com este e-mail.");
            }
        }

        UserMapper.updateEntity(user, dto);

        // se veio senha, atualiza hash
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            user.setSenhaHash(passwordEncoder.encode(dto.getSenha()));
        }

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void inactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        user.setAtivo(false);
        userRepository.save(user);
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

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        boolean isFuncionario = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_FUNCIONARIO".equals(a.getAuthority()));

        // CLIENTE não lista usuários
        if (!isAdmin && !isFuncionario) {
            throw new ForbiddenException("Sem permissão para listar usuários.");
        }

        // Regra: FUNCIONARIO só pode listar CLIENTE
        if (isFuncionario) {
            if (perfil != null && perfil != PerfilUser.CLIENTE) {
                throw new ForbiddenException("Funcionário só pode listar clientes.");
            }
            perfil = PerfilUser.CLIENTE; // força o filtro, mesmo se não vier perfil
        }

        // Defaults e proteção básica
        int safePage = (page == null || page < 1) ? 1 : page;
        int safeLimit = (limit == null || limit < 1) ? 10 : Math.min(limit, 100);

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeLimit,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<User> result = userRepository.search(perfil, ativo, q, pageable);

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
}
