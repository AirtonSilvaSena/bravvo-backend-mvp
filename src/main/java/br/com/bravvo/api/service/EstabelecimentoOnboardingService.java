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

	/**
	 * MULTI-TENANT (identidade por estabelecimento):
	 * - Mesmo e-mail/telefone pode existir em estabelecimentos diferentes.
	 * - No pré-cadastro, o "dono" do processo é o SLUG (tenant).
	 * - Portanto:
	 *   - NÃO validar e-mail/telefone global em users.
	 *   - O pré-cadastro deve ser encontrado/atualizado por SLUG (único).
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

		// Slug já existe em estabelecimentos (salão já criado) -> bloqueia
		if (salaoRepository.existsBySlug(slug)) {
			throw new BusinessException("Endereço de estabelecimento já está em uso.");
		}

		// gera novo código SEMPRE (para sobrescrever e reenviar)
		String codigo = VerificationCodeUtils.generate6Digits();
		String codigoHash = TokenHashUtils.sha256(codigo);

		// PIVOT pelo SLUG (uk_est_pre_slug)
		EstabelecimentosPreCadastro pre = preCadastroRepository.findBySlug(slug).orElse(null);

		if (pre == null) {
			pre = new EstabelecimentosPreCadastro();
			pre.setSlug(slug);
			pre.setAttempts(0);
		} else {
			// já existe pré-cadastro pendente para este slug -> sobrescreve e reseta tentativas
			pre.setAttempts(0);
		}

		pre.setNome(dto.getNome().trim());
		pre.setRamoAtuacao(dto.getRamoAtuacao().trim());
		pre.setEmail(email);
		pre.setTelefone(telefoneNorm); // normalizado (pode repetir em outros slugs)
		pre.setSenhaHash(passwordEncoder.encode(dto.getSenha()));

		pre.setCodigoHash(codigoHash);
		pre.setExpiresAt(LocalDateTime.now().plusMinutes(15));

		preCadastroRepository.save(pre);

		return new PreRegisterResult(email, codigo);
	}

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

	    // Agora que email pode repetir no pré-cadastro, confirmamos NO CONTEXTO DO SLUG
	    EstabelecimentosPreCadastro pre = preCadastroRepository.findBySlug(slug)
	            .orElseThrow(() -> new BusinessException("Pré-cadastro não encontrado. Solicite um novo código."));

	    // Garante que o email informado é o mesmo do pré-cadastro deste slug
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

	    // Race condition: slug não pode ter virado um estabelecimento entre preRegister e confirm
	    if (salaoRepository.existsBySlug(pre.getSlug())) {
	        preCadastroRepository.delete(pre);
	        throw new BusinessException("Endereço de estabelecimento já está em uso.");
	    }

	    // 1) Cria salão
	    Estabelecimentos estabelecimento = new Estabelecimentos();
	    estabelecimento.setNome(pre.getNome());
	    estabelecimento.setTelefone(pre.getTelefone());
	    estabelecimento.setRamoAtuacao(pre.getRamoAtuacao());
	    estabelecimento.setSlug(pre.getSlug());
	    estabelecimento.setStatusAssinatura(StatusAssinatura.TRIAL);
	    estabelecimento.setTrialEndsAt(LocalDateTime.now().plusDays(14));
	    salaoRepository.save(estabelecimento);

	    Long estId = estabelecimento.getId();

	    // 2) Cria user ADMIN (multi-tenant: valida por estabelecimento)
	    // (Opcional, mas deixa mensagem amigável. Em corrida, o UNIQUE do banco garante)
	    if (userRepository.existsByEstabelecimentoIdAndEmail(estId, email)) {
	        throw new BusinessException("E-mail já cadastrado neste estabelecimento.");
	    }
	    if (pre.getTelefone() != null && !pre.getTelefone().isBlank()
	            && userRepository.existsByEstabelecimentoIdAndTelefone(estId, pre.getTelefone())) {
	        throw new BusinessException("Telefone já cadastrado neste estabelecimento.");
	    }

	    User admin = new User();
	    admin.setNome(pre.getNome());
	    admin.setEmail(email);
	    admin.setTelefone(pre.getTelefone());
	    admin.setSenhaHash(pre.getSenhaHash());
	    admin.setPerfil(PerfilUser.ADMIN);
	    admin.setAtivo(true);
	    admin.setEmailVerificado(true);
	    admin.setSalaoId(estId);

	    userRepository.save(admin);

	    // 3) Define owner do salão
	    estabelecimento.setOwnerUser(admin);
	    salaoRepository.save(estabelecimento);

	    // 4) Apaga pré-cadastro (por slug)
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