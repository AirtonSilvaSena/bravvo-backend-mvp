package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoConfirmEmailRequestDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoPreRegisterRequestDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.EstabelecimentosPreCadastro;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.enums.StatusAssinatura;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.repository.EstabelecimentoPreCadastroRepository;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
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
	private final PasswordEncoder passwordEncoder;
	private final MailService mailService;

	public EstabelecimentoOnboardingService(
			EstabelecimentoRepository salaoRepository,
			EstabelecimentoPreCadastroRepository preCadastroRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			MailService mailService
	) {
		this.salaoRepository = salaoRepository;
		this.preCadastroRepository = preCadastroRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.mailService = mailService;
	}
	
	public void preRegister(EstabelecimentoPreRegisterRequestDTO dto) {
	    PreRegisterResult result = preRegisterTransactional(dto);
	    mailService.sendVerificationCode(result.email, result.codigo);
	}

	@Transactional
	protected PreRegisterResult preRegisterTransactional(EstabelecimentoPreRegisterRequestDTO dto) {

	    String slug = SlugUtils.normalize(dto.getSlug());
	    if (!SlugUtils.isValid(slug)) {
	        throw new BusinessException("Slug inválido. Use letras minúsculas, números e hífen.");
	    }

	    String email = dto.getEmail().trim().toLowerCase();

	    String telefoneNorm = null;
	    if (dto.getTelefone() != null && !dto.getTelefone().isBlank()) {
	        telefoneNorm = dto.getTelefone().trim().replaceAll("\\D", "");
	    }

	    // Email já cadastrado em users?
	    if (userRepository.existsByEmail(email)) {
	        throw new BusinessException("E-mail já cadastrado.");
	    }

	    // Telefone já cadastrado em users? (admin ou funcionário)
	    if (telefoneNorm != null && userRepository.existsByTelefone(telefoneNorm)) {
	        throw new BusinessException("Telefone já cadastrado.");
	    }

	    // Slug já existe em saloes (salão já criado)
	    if (salaoRepository.existsBySlug(slug)) {
	        throw new BusinessException("Slug já está em uso.");
	    }

	    // gera novo código SEMPRE (para sobrescrever)
	    String codigo = VerificationCodeUtils.generate6Digits();
	    String codigoHash = TokenHashUtils.sha256(codigo);

	    // tenta pegar pré-cadastro existente pelo email
	    EstabelecimentosPreCadastro pre = preCadastroRepository.findByEmail(email).orElse(null);

	    // Telefone já está em outro pré-cadastro?
	    if (telefoneNorm != null) {
	        EstabelecimentosPreCadastro preByTelefone = preCadastroRepository.findByTelefone(telefoneNorm).orElse(null);

	        if (preByTelefone != null) {
	            // se é outro registro (outro email), bloqueia
	            if (pre == null || !preByTelefone.getId().equals(pre.getId())) {
	                throw new BusinessException("Já existe um pré-cadastro pendente para este telefone.");
	            }
	        }
	    }

	    if (pre == null) {
	        var preBySlug = preCadastroRepository.findBySlug(slug).orElse(null);
	        if (preBySlug != null) {
	            throw new BusinessException("Slug já está em uso (pré-cadastro pendente).");
	        }

	        pre = new EstabelecimentosPreCadastro();
	        pre.setEmail(email);
	        pre.setAttempts(0);
	    } else {
	        if (!slug.equals(pre.getSlug())) {
	            var preBySlug = preCadastroRepository.findBySlug(slug).orElse(null);
	            if (preBySlug != null && !preBySlug.getId().equals(pre.getId())) {
	                throw new BusinessException("Slug já está em uso (pré-cadastro pendente).");
	            }
	        }
	        pre.setAttempts(0);
	    }

	    pre.setNome(dto.getNome().trim());
	    pre.setRamoAtuacao(dto.getRamoAtuacao().trim());
	    pre.setTelefone(telefoneNorm); // <- SALVA NORMALIZADO
	    pre.setSlug(slug);

	    String senhaHash = passwordEncoder.encode(dto.getSenha());
	    pre.setSenhaHash(senhaHash);

	    pre.setCodigoHash(codigoHash);
	    pre.setExpiresAt(LocalDateTime.now().plusMinutes(15));

	    preCadastroRepository.save(pre);

	    return new PreRegisterResult(email, codigo);
	}

	@Transactional
	public void confirmEmail(EstabelecimentoConfirmEmailRequestDTO dto) {

		String email = dto.getEmail().trim().toLowerCase();
		String codigo = dto.getCodigo().trim();

		EstabelecimentosPreCadastro pre = preCadastroRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException("Pré-cadastro não encontrado. Solicite um novo código."));

		if (pre.getExpiresAt().isBefore(LocalDateTime.now())) {
			preCadastroRepository.deleteByEmail(email);
			throw new BusinessException("Código expirado. Solicite um novo cadastro.");
		}

		if (pre.getAttempts() >= 5) {
			preCadastroRepository.deleteByEmail(email);
			throw new BusinessException("Muitas tentativas. Refazer cadastro.");
		}

		String codigoHash = TokenHashUtils.sha256(codigo);
		if (!codigoHash.equals(pre.getCodigoHash())) {
			pre.setAttempts(pre.getAttempts() + 1);
			preCadastroRepository.save(pre);
			throw new BusinessException("Código inválido.");
		}

		// Revalida slug/email no momento da confirmação (race condition)
		if (userRepository.existsByEmail(email)) {
			preCadastroRepository.deleteByEmail(email);
			throw new BusinessException("E-mail já cadastrado.");
		}
		if (salaoRepository.existsBySlug(pre.getSlug())) {
			preCadastroRepository.deleteByEmail(email);
			throw new BusinessException("Slug já está em uso.");
		}

		// 1) Cria salão (trial começa aqui)
		Estabelecimentos estabelecimento = new Estabelecimentos();
		estabelecimento.setNome(pre.getNome());
		estabelecimento.setTelefone(pre.getTelefone());
		estabelecimento.setRamoAtuacao(pre.getRamoAtuacao());
		estabelecimento.setSlug(pre.getSlug());
		estabelecimento.setStatusAssinatura(StatusAssinatura.TRIAL);
		estabelecimento.setTrialEndsAt(LocalDateTime.now().plusDays(14));

		salaoRepository.save(estabelecimento);

		// 2) Cria user ADMIN
		User admin = new User();
		admin.setNome(pre.getNome()); // ou "Admin do salão" — você escolhe depois
		admin.setEmail(email);
		admin.setTelefone(pre.getTelefone());
		admin.setSenhaHash(pre.getSenhaHash());
		admin.setPerfil(PerfilUser.ADMIN);
		admin.setAtivo(true);

		// campos novos no users:
		admin.setEmailVerificado(true);
		admin.setSalaoId(estabelecimento.getId());

		userRepository.save(admin);

		// 3) Define owner do salão
		estabelecimento.setOwnerUser(admin);
		salaoRepository.save(estabelecimento);

		// 4) Apaga pré-cadastro
		preCadastroRepository.deleteByEmail(email);
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