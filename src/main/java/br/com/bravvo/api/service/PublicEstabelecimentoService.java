package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.estabelecimento.PublicEstabelecimentoPublicoResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.PublicEstabelecimentoResolveResponseDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.service.storage.EstabelecimentoLogoStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class PublicEstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoLogoStorageService logoStorageService;

    public PublicEstabelecimentoService(EstabelecimentoRepository estabelecimentoRepository,
                                        EstabelecimentoLogoStorageService logoStorageService) {
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.logoStorageService = logoStorageService;
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
                estab.getInstagramUrl()
        );
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