package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.agendamento.PublicEstabelecimentoEmailResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.PublicEstabelecimentoPublicoResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.PublicEstabelecimentoResolveResponseDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.service.storage.EstabelecimentoLogoStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Service
public class PublicEstabelecimentoService {
	
	private final UserRepository userRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoLogoStorageService logoStorageService;

    public PublicEstabelecimentoService(EstabelecimentoRepository estabelecimentoRepository,
                                        EstabelecimentoLogoStorageService logoStorageService, UserRepository userRepository) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.logoStorageService = logoStorageService;
        this.userRepository = userRepository;
    }

    /**
     * Resolver estabelecimento por slug (branding/status) para tela de login e navegação pública.
     * NÃO ALTERAR este método para evitar impacto no comportamento já integrado.
     */
    public PublicEstabelecimentoResolveResponseDTO resolveBySlug(String slug) {
        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        String logoUrl = null;
        if (estab.getLogoKey() != null && !estab.getLogoKey().isBlank()) {
            long v = 0L;
            if (estab.getLogoUpdatedAt() != null) {
                v = estab.getLogoUpdatedAt().toEpochSecond(ZoneOffset.UTC);
            }
            logoUrl = "/api/public/estabelecimentos/" + estab.getSlug() + "/logo?v=" + v;
        }

        return new PublicEstabelecimentoResolveResponseDTO(
                estab.getSlug(),
                estab.getNome(),
                estab.getStatusAssinatura() != null ? estab.getStatusAssinatura().name() : null,
                estab.getTrialEndsAt(),
                logoUrl
        );
    }

    /**
     * Retorna informações públicas "mais completas" do estabelecimento (para páginas públicas, como agendamento).
     * - Não exige autenticação.
     * - Não expõe dados internos.
     */
    public PublicEstabelecimentoPublicoResponseDTO getPublicoBySlug(String slug) {
        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        String logoUrl = null;
        if (estab.getLogoKey() != null && !estab.getLogoKey().isBlank()) {
            long v = 0L;
            if (estab.getLogoUpdatedAt() != null) {
                v = estab.getLogoUpdatedAt().toEpochSecond(ZoneOffset.UTC);
            }
            logoUrl = "/api/public/estabelecimentos/" + estab.getSlug() + "/logo?v=" + v;
        }

        return new PublicEstabelecimentoPublicoResponseDTO(
                estab.getSlug(),
                estab.getNome(),
                estab.getTelefone(),
                estab.getRamoAtuacao(),
                estab.getEndereco(),
                estab.getNumero(),
                estab.getBairro(),
                estab.getCidade(),
                estab.getEstado(),               
                estab.getStatusAssinatura() != null ? estab.getStatusAssinatura().name() : null,
                estab.getTrialEndsAt(),
                logoUrl,
                estab.getSobreNos(),
                estab.getInstagramUrl(),
                estab.getCep()
        );
    }
    
    /**
     * Retorna os estabelecimentos (nome + slug) vinculados a um e-mail.
     *
     * Regra:
     * - Busca usuário global por e-mail
     * - Retorna todos estabelecimentos ativos vinculados a ele
     */
    @Transactional(readOnly = true)
    public List<PublicEstabelecimentoEmailResponseDTO> getSlugsByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new BusinessException("Informe o e-mail.");
        }

        String emailNormalizado = email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        List<Estabelecimentos> estabelecimentos =
                estabelecimentoRepository.findAllAtivosByUserId(user.getId());

        if (estabelecimentos.isEmpty()) {
            throw new NotFoundException("Nenhum estabelecimento vinculado a este usuário.");
        }

        return estabelecimentos.stream()
                .map(estab -> new PublicEstabelecimentoEmailResponseDTO(
                        estab.getNome(),
                        estab.getSlug()
                ))
                .toList();
    }
    
    public ResponseEntity<byte[]> getLogoBySlug(String slug) {
        Estabelecimentos estab = estabelecimentoRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado"));

        // Aqui a gente permite servir logo mesmo INADIMPLENTE/CANCELADO (branding).
        if (estab.getLogoKey() == null || estab.getLogoKey().isBlank()) {
            throw new NotFoundException("Logo não encontrada");
        }

        return logoStorageService.buildLogoResponse(
                estab.getLogoKey(),
                estab.getLogoMimeType(),
                estab.getLogoUpdatedAt()
        );
    }
}