package br.com.bravvo.api.service;

import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.EstabelecimentoUser;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.enums.PerfilUser;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.EstabelecimentoUserRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class EstabelecimentoLogoAdminService {

    private static final long MAX_BYTES = 500 * 1024; // 500KB
    private static final Set<String> ALLOWED_MIME = Set.of("image/png", "image/jpeg");

    private final UserRepository userRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EstabelecimentoUserRepository estabelecimentoUserRepository;

    @Value("${app.storage.logos-dir:/app/storage/logos}")
    private String logosDir;

    public EstabelecimentoLogoAdminService(
            UserRepository userRepository,
            EstabelecimentoRepository estabelecimentoRepository,
            EstabelecimentoUserRepository estabelecimentoUserRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.estabelecimentoUserRepository = estabelecimentoUserRepository;
    }

    /**
     * Upload (sobrescreve) do logo do estabelecimento do ADMIN logado (tenant atual).
     */
    @Transactional
    public void uploadLogo(MultipartFile file) {
        AuthCtx ctx = getAuthenticatedAdminCtxOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findById(ctx.estabelecimentoId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        validateFile(file);

        String mime = file.getContentType();
        String ext = "image/png".equals(mime) ? "png" : "jpg";
        String filename = "estabelecimento-" + est.getId() + "." + ext;

        try {
            Path dir = Paths.get(logosDir).normalize();
            Files.createDirectories(dir);

            Path target = dir.resolve(filename).normalize();

            // Proteção contra path traversal
            if (!target.startsWith(dir)) {
                throw new BusinessException("Caminho inválido para salvar o logo.");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            est.setLogoKey(filename);
            est.setLogoMimeType(mime);
            est.setLogoSizeBytes(file.getSize());
            est.setLogoUpdatedAt(LocalDateTime.now());

            estabelecimentoRepository.save(est);

        } catch (IOException ex) {
            throw new BusinessException("Falha ao salvar o logo no disco.");
        }
    }

    /**
     * Retorna o logo do estabelecimento do ADMIN logado (tenant atual).
     */
    public ResponseEntity<byte[]> getLogoResponse() {
        AuthCtx ctx = getAuthenticatedAdminCtxOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findById(ctx.estabelecimentoId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        if (est.getLogoKey() == null || est.getLogoKey().isBlank()) {
            throw new NotFoundException("Este estabelecimento ainda não possui logo.");
        }

        try {
            Path dir = Paths.get(logosDir).normalize();
            Path filePath = dir.resolve(est.getLogoKey()).normalize();

            // Proteção contra path traversal
            if (!filePath.startsWith(dir)) {
                throw new NotFoundException("Logo não encontrado.");
            }

            if (!Files.exists(filePath)) {
                throw new NotFoundException("Logo não encontrado no disco.");
            }

            byte[] bytes = Files.readAllBytes(filePath);

            MediaType mediaType = "image/png".equals(est.getLogoMimeType())
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                    .body(bytes);

        } catch (IOException ex) {
            throw new NotFoundException("Falha ao ler o logo.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo obrigatório.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Logo muito grande. Máximo permitido: 500KB.");
        }

        String mime = file.getContentType();
        if (!StringUtils.hasText(mime) || !ALLOWED_MIME.contains(mime)) {
            throw new BusinessException("Formato inválido. Envie PNG ou JPG.");
        }
    }

    /**
     * Multi-tenant (novo padrão):
     * - Tenant vem do JWT (TenantContext).
     * - Perfil vem do vínculo estabelecimento_users.
     * - Validamos: vínculo ativo + usuário ativo + perfil ADMIN no tenant atual.
     */
    private AuthCtx getAuthenticatedAdminCtxOrThrow() {
        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();
        String email = TenantContext.getEmailOrThrow();

        // 1) Vínculo no tenant atual (fonte de verdade para perfil)
        EstabelecimentoUser vinculo = estabelecimentoUserRepository
                .findByEstabelecimentoIdAndEmail(estabelecimentoId, email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado neste estabelecimento."));

        // 2) Vínculo ativo (se você estiver usando esse campo pra desativar vínculo)
        if (Boolean.FALSE.equals(vinculo.getAtivo())) {
            throw new ForbiddenException("Vínculo inativo neste estabelecimento.");
        }

        // 3) Perfil ADMIN no vínculo
        if (vinculo.getPerfil() == null || vinculo.getPerfil() != PerfilUser.ADMIN) {
            throw new ForbiddenException("Acesso permitido somente para ADMIN.");
        }

        // 4) User ativo (estado global do usuário)
        User user = userRepository.findById(vinculo.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário inválido para este vínculo."));

        if (Boolean.FALSE.equals(user.getAtivo())) {
            throw new ForbiddenException("Usuário inativo.");
        }

        return new AuthCtx(estabelecimentoId, email, user.getId(), vinculo.getId());
    }

    /**
     * Contexto mínimo (ajuda debug/log e deixa explícito o tenant atual).
     */
    private record AuthCtx(Long estabelecimentoId, String email, Long userId, Long vinculoId) {}
}