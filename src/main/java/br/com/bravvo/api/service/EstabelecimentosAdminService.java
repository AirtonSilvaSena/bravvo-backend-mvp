package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeUpdateRequestDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
public class EstabelecimentosAdminService {

    private final UserRepository userRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;

    public EstabelecimentosAdminService(
            UserRepository userRepository,
            EstabelecimentoRepository estabelecimentoRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
    }

    public EstabelecimentoMeResponseDTO getMe() {
        // valida auth + tenant + ADMIN via vínculo
        AuthCtx ctx = getAuthenticatedAdminCtxOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findById(ctx.estabelecimentoId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        return toMeResponse(est);
    }

    @Transactional
    public EstabelecimentoMeResponseDTO updateMe(EstabelecimentoMeUpdateRequestDTO dto) {
        // valida auth + tenant + ADMIN via vínculo
        AuthCtx ctx = getAuthenticatedAdminCtxOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findById(ctx.estabelecimentoId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        // Atualiza somente campos permitidos (slug e assinatura ficam imutáveis aqui)
        est.setNome(dto.getNome().trim());
        est.setTelefone(dto.getTelefone() == null ? null : dto.getTelefone().trim());
        est.setRamoAtuacao(dto.getRamoAtuacao() == null ? null : dto.getRamoAtuacao().trim());
        est.setEndereco(dto.getEndereco() == null ? null : dto.getEndereco().trim());
        est.setNumero(dto.getNumero() == null ? null : dto.getNumero().trim());
        est.setBairro(dto.getBairro() == null ? null : dto.getBairro().trim());
        est.setEstado(dto.getEstado() == null ? null : dto.getEstado().trim());
        est.setCidade(dto.getCidade() == null ? null : dto.getCidade().trim());
        est.setSobreNos(dto.getSobreNos() == null ? null : dto.getSobreNos().trim());
        est.setInstagramUrl(dto.getInstagramUrl() == null ? null : dto.getInstagramUrl().trim());
        est.setCep(dto.getCep() == null ? null : dto.getCep().trim());

        estabelecimentoRepository.save(est);

        return toMeResponse(est);
    }

    /**
     * Multi-tenant (padrão novo):
     * - Tenant vem do JWT (TenantContext).
     * - Perfil vem do vínculo estabelecimento_users.
     * - Valida: vínculo ativo + user ativo + perfil ADMIN.
     */
    private AuthCtx getAuthenticatedAdminCtxOrThrow() {
        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        // 1) encontra vínculo do usuário logado no tenant atual (por email -> userId)
        // Como estabelecimento_users não tem email, isso precisa ser um @Query com JOIN em users.
        EstabelecimentoUser vinculo = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado neste estabelecimento."));

        // 2) vínculo ativo
        if (Boolean.FALSE.equals(vinculo.getAtivo())) {
            throw new ForbiddenException("Usuário sem permissão (vínculo inativo).");
        }

        // 3) perfil ADMIN
        if (vinculo.getPerfil() != PerfilUser.ADMIN) {
            throw new ForbiddenException("Acesso permitido somente para ADMIN.");
        }

        // 4) user global ativo
        User user = userRepository.findById(vinculo.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário inválido para este vínculo."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        return new AuthCtx(estabelecimentoId, user.getId(), vinculo.getId(), email);
    }

    /**
     * Monta a URL do logo (endpoint interno) com cache-busting via logoUpdatedAt.
     * Retorna null se não houver logo salvo.
     */
    private String buildLogoUrl(Estabelecimentos e) {
        if (e.getLogoKey() == null || e.getLogoKey().isBlank()) return null;

        String base = "/api/admin/estabelecimento/me/logo";

        if (e.getLogoUpdatedAt() == null) return base;

        long v = e.getLogoUpdatedAt().toEpochSecond(ZoneOffset.UTC);
        return base + "?v=" + v;
    }

    private EstabelecimentoMeResponseDTO toMeResponse(Estabelecimentos e) {
        EstabelecimentoMeResponseDTO dto = new EstabelecimentoMeResponseDTO();

        dto.setId(e.getId());
        dto.setNome(e.getNome());
        dto.setTelefone(e.getTelefone());
        dto.setRamoAtuacao(e.getRamoAtuacao());
        dto.setEndereco(e.getEndereco());
        dto.setNumero(e.getNumero());
        dto.setBairro(e.getBairro());
        dto.setEstado(e.getEstado());
        dto.setCidade(e.getCidade());
        dto.setSlug(e.getSlug());
        dto.setSobreNos(e.getSobreNos());
        dto.setInstagramUrl(e.getInstagramUrl());
        dto.setCep(e.getCep());

        Object status = e.getStatusAssinatura();
        dto.setStatusAssinatura(status == null ? null : status.toString());

        dto.setTrialEndsAt(e.getTrialEndsAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());

        dto.setLogoUrl(buildLogoUrl(e));

        return dto;
    }

    private record AuthCtx(Long estabelecimentoId, Long userId, Long vinculoId, String email) {}
}