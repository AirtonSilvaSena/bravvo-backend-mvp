package br.com.bravvo.api.service;

import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeResponseDTO;
import br.com.bravvo.api.dto.estabelecimento.EstabelecimentoMeUpdateRequestDTO;
import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class EstabelecimentosAdminService {

    private final UserRepository userRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public EstabelecimentosAdminService(
            UserRepository userRepository,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    public EstabelecimentoMeResponseDTO getMe() {
        User admin = getAuthenticatedAdminOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findByOwnerUserId(admin.getId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado para este admin."));

        return toMeResponse(est);
    }

    @Transactional
    public EstabelecimentoMeResponseDTO updateMe(EstabelecimentoMeUpdateRequestDTO dto) {
        User admin = getAuthenticatedAdminOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findByOwnerUserId(admin.getId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado para este admin."));

        // Atualiza somente campos permitidos (slug e assinatura ficam imutáveis aqui)
        est.setNome(dto.getNome().trim());
        est.setTelefone(dto.getTelefone() == null ? null : dto.getTelefone().trim());
        est.setRamoAtuacao(dto.getRamoAtuacao() == null ? null : dto.getRamoAtuacao().trim());
        est.setEndereco(dto.getEndereco() == null ? null : dto.getEndereco().trim());
        est.setNumero(dto.getNumero() == null ? null : dto.getNumero().trim());
        est.setBairro(dto.getBairro() == null ? null : dto.getBairro().trim());
        est.setEstado(dto.getEstado() == null ? null : dto.getEstado().trim());
        est.setCidade(dto.getCidade() == null ? null : dto.getCidade().trim());

        estabelecimentoRepository.save(est);

        return toMeResponse(est);
    }

    private User getAuthenticatedAdminOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Usuário não autenticado.");
        }

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        if (user.getPerfil() != PerfilUser.ADMIN) {
            throw new ForbiddenException("Acesso permitido somente para ADMIN.");
        }

        return user;
    }

    /**
     * Monta o DTO do estabelecimento sem usar Mapper.
     * Mantém o contrato do frontend e evita erro de construtor.
     */
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

        // statusAssinatura pode ser String ou Enum dependendo da sua entity
        Object status = e.getStatusAssinatura();
        dto.setStatusAssinatura(status == null ? null : status.toString());

        dto.setTrialEndsAt(e.getTrialEndsAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());

        return dto;
    }
}
