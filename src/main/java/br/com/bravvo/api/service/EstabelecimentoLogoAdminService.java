package br.com.bravvo.api.service;

import br.com.bravvo.api.entity.Estabelecimentos;
import br.com.bravvo.api.entity.User;
import br.com.bravvo.api.exception.BusinessException;
import br.com.bravvo.api.exception.ForbiddenException;
import br.com.bravvo.api.exception.NotFoundException;
import br.com.bravvo.api.repository.EstabelecimentoRepository;
import br.com.bravvo.api.repository.UserRepository;
import br.com.bravvo.api.security.TenantContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class EstabelecimentoLogoAdminService {

    private static final long MAX_BYTES = 500 * 1024; // 500KB
    private static final Set<String> ALLOWED_MIME = Set.of("image/png", "image/jpeg");

    private final UserRepository userRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    @Value("${app.storage.logos-dir:/app/storage/logos}")
    private String logosDir;

    public EstabelecimentoLogoAdminService(
            UserRepository userRepository,
            EstabelecimentoRepository estabelecimentoRepository
    ) {
        this.userRepository = userRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    @Transactional
    public void uploadLogo(MultipartFile file) {
        User admin = getAuthenticatedAdminOrThrow();

        Long estabelecimentoId = TenantContext.getEstabelecimentoIdOrThrow();

        // opcional (recomendado): garante que o token é do mesmo tenant do usuário
        if (admin.getEstabelecimentoId() == null || !admin.getEstabelecimentoId().equals(estabelecimentoId)) {
            throw new ForbiddenException("Token não pertence a este estabelecimento.");
        }

        Estabelecimentos est = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado."));

        validateFile(file);

        String mime = file.getContentType();
        String ext = "image/png".equals(mime) ? "png" : "jpg";

        String filename = "estabelecimento-" + est.getId() + "." + ext;

        try {
            Path dir = Paths.get(logosDir);
            Files.createDirectories(dir);

            Path target = dir.resolve(filename).normalize();

            if (!target.startsWith(dir.normalize())) {
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

    public ResponseEntity<byte[]> getLogoResponse() {
        User admin = getAuthenticatedAdminOrThrow();

        Estabelecimentos est = estabelecimentoRepository.findByOwnerUserId(admin.getId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento não encontrado para este admin."));

        if (est.getLogoKey() == null || est.getLogoKey().isBlank()) {
            throw new NotFoundException("Este estabelecimento ainda não possui logo.");
        }

        try {
            Path dir = Paths.get(logosDir);
            Path filePath = dir.resolve(est.getLogoKey()).normalize();

            if (!filePath.startsWith(dir.normalize())) {
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
                    // cache forte; o cache-busting é via ?v=logoUpdatedAt
                    .cacheControl(CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic())
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

        // você já tem PerfilUser.ADMIN; mantive checagem aqui para não depender da outra service
        if (user.getPerfil() == null || !"ADMIN".equals(user.getPerfil().name())) {
            throw new ForbiddenException("Acesso permitido somente para ADMIN.");
        }

        return user;
    }
}
