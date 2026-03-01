package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoConfirmEmailRequestDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoPreRegisterRequestDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.EstabelecimentosPreCadastro;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusAssinatura;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.repository.EstabelecimentoPreCadastroRepository;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.util.SlugUtils;
import br.com.bravvo.api.util.TokenHashUtils;
import br.com.bravvo.api.util.VerificationCodeUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EstabelecimentoOnboardingService {

    private final EstabelecimentoRepository salaoRepository;
    private final EstabelecimentoPreCadastroRepository preCadastroRepository;
    private final UserRepository userRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public EstabelecimentoOnboardingService(
            EstabelecimentoRepository salaoRepository,
            EstabelecimentoPreCadastroRepository preCadastroRepository,
            UserRepository userRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService
    ) {
        this.salaoRepository = salaoRepository;
        this.preCadastroRepository = preCadastroRepository;
        this.userRepository = userRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public void preRegister(EstabelecimentoPreRegisterRequestDTO dto) {
        PreRegisterResult result = preRegisterTransactional(dto);
        mailService.sendVerificationCode(result.email, result.codigo);
    }

    /**
     * MULTI-TENANT (identidade por estabelecimento):
     * - No pré-cadastro, o pivot é o SLUG do estabelecimento.
     * - O objetivo é confirmar e-mail para criar um novo estabelecimento.
     */
    @Transactional
    protected PreRegisterResult preRegisterTransactional(EstabelecimentoPreRegisterRequestDTO dto) {

        String slug = SlugUtils.normalize(dto.getSlug());
        if (!SlugUtils.isValid(slug)) {
            throw new BusinessException("Endereço do estabelecimento inválido. Use letras minúsculas, números e hífen.");
        }

        String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase();
        if (email.isBlank()) {
            throw new BusinessException("E-mail é obrigatório.");
        }

        String telefoneNorm = null;
        if (dto.getTelefone() != null && !dto.getTelefone().isBlank()) {
            telefoneNorm = dto.getTelefone().trim().replaceAll("\\D", "");
        }

        // slug já existe em estabelecimentos -> bloqueia
        if (salaoRepository.existsBySlug(slug)) {
            throw new BusinessException("Endereço de estabelecimento já está em uso.");
        }

        // gera novo código SEMPRE (para sobrescrever e reenviar)
        String codigo = VerificationCodeUtils.generate6Digits();
        String codigoHash = TokenHashUtils.sha256(codigo);

        // Pivot pelo SLUG (uk_est_pre_slug)
        EstabelecimentosPreCadastro pre = preCadastroRepository.findBySlug(slug).orElse(null);

        if (pre == null) {
            pre = new EstabelecimentosPreCadastro();
            pre.setSlug(slug);
            pre.setAttempts(0);
        } else {
            pre.setAttempts(0);
        }

        pre.setNome(dto.getNome().trim());
        pre.setNomeOwner(dto.getNomeOwner().trim());
        pre.setRamoAtuacao(dto.getRamoAtuacao().trim());
        pre.setEmail(email);
        pre.setTelefone(telefoneNorm);
        pre.setSenhaHash(passwordEncoder.encode(dto.getSenha()));

        pre.setCodigoHash(codigoHash);
        pre.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        preCadastroRepository.save(pre);

        return new PreRegisterResult(email, codigo);
    }

    /**
     * Confirma e-mail e cria:
     * 1) Estabelecimento (tenant)
     * 2) User (global) -> REUSA se já existir por e-mail (evita conflito UNIQUE)
     * 3) Vínculo estabelecimento_users (ADMIN)
     * 4) owner_user do estabelecimento
     */
    @Transactional
    public void confirmEmail(EstabelecimentoConfirmEmailRequestDTO dto) {

        String slug = SlugUtils.normalize(dto.getSlug());
        if (!SlugUtils.isValid(slug)) {
            throw new BusinessException("Endereço do estabelecimento inválido.");
        }

        String email = dto.getEmail() == null ? "" : dto.getEmail().trim().toLowerCase();
        if (email.isBlank()) {
            throw new BusinessException("E-mail é obrigatório.");
        }

        String codigo = dto.getCodigo() == null ? "" : dto.getCodigo().trim();
        if (codigo.isBlank()) {
            throw new BusinessException("Código é obrigatório.");
        }

        // Confirma no contexto do SLUG
        EstabelecimentosPreCadastro pre = preCadastroRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Pré-cadastro não encontrado. Solicite um novo código."));

        if (!email.equalsIgnoreCase(pre.getEmail())) {
            throw new BusinessException("E-mail não confere com o pré-cadastro deste estabelecimento.");
        }

        if (pre.getExpiresAt().isBefore(LocalDateTime.now())) {
            preCadastroRepository.delete(pre);
            throw new BusinessException("Código expirado. Solicite um novo cadastro.");
        }

        if (pre.getAttempts() >= 5) {
            preCadastroRepository.delete(pre);
            throw new BusinessException("Muitas tentativas. Refazer cadastro.");
        }

        String codigoHash = TokenHashUtils.sha256(codigo);
        if (!codigoHash.equals(pre.getCodigoHash())) {
            pre.setAttempts(pre.getAttempts() + 1);
            preCadastroRepository.save(pre);
            throw new BusinessException("Código inválido.");
        }

        // Race: slug pode ter virado estabelecimento entre preRegister e confirm
        if (salaoRepository.existsBySlug(pre.getSlug())) {
            preCadastroRepository.delete(pre);
            throw new BusinessException("Endereço de estabelecimento já está em uso.");
        }

        // 1) Cria estabelecimento
        Estabelecimentos estabelecimento = new Estabelecimentos();
        estabelecimento.setNome(pre.getNome());
        estabelecimento.setTelefone(pre.getTelefone());
        estabelecimento.setRamoAtuacao(pre.getRamoAtuacao());
        estabelecimento.setSlug(pre.getSlug());
        estabelecimento.setStatusAssinatura(StatusAssinatura.TRIAL);
        estabelecimento.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        salaoRepository.save(estabelecimento);

        Long estId = estabelecimento.getId();

        // 2) User (GLOBAL) - REUSA se já existir por e-mail
        // Observação:
        // - Isso elimina o 409 de UNIQUE(email).
        // - Se já existir, NÃO trocamos senha automaticamente (evita “sequestro” de conta).
        User admin = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (admin == null) {
            // cria novo usuário
            admin = new User();
            admin.setNome(pre.getNomeOwner());
            admin.setEmail(email);
            admin.setSenhaHash(pre.getSenhaHash()); // já está hash no pré-cadastro
            admin.setAtivo(true);
            admin.setEmailVerificado(true);
            admin = userRepository.save(admin);
        } else {
            // validações mínimas
            if (!Boolean.TRUE.equals(admin.getAtivo())) {
                throw new BusinessException("Este e-mail está vinculado a um usuário inativo.");
            }

        }

        // 3) Cria vínculo ADMIN no tenant
        // Aqui é onde validamos duplicidade POR ESTABELECIMENTO.
        if (estabelecimentoUserRepository.existsByEstabelecimentoIdAndUserId(estId, admin.getId())) {
            throw new BusinessException("Usuário já vinculado a este estabelecimento.");
        }

        EstabelecimentoUser vinculo = new EstabelecimentoUser();
        vinculo.setEstabelecimentoId(estId);
        vinculo.setUserId(admin.getId());
        vinculo.setPerfil(PerfilUser.ADMIN);
        vinculo.setAtivo(true);
        estabelecimentoUserRepository.save(vinculo);

        // 4) Define owner do estabelecimento (admin global)
        estabelecimento.setOwnerUser(admin);
        salaoRepository.save(estabelecimento);

        // 5) Apaga pré-cadastro
        preCadastroRepository.delete(pre);
    }

    private static class PreRegisterResult {
        final String email;
        final String codigo;

        private PreRegisterResult(String email, String codigo) {
            this.email = email;
            this.codigo = codigo;
        }
    }
}