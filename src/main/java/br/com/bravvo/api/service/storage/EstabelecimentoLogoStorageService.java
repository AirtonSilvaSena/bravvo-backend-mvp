package br.com.bravvo.api.service.storage;

import br.com.bravvo.api.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class EstabelecimentoLogoStorageService {

	private final Path logosDir;

	public EstabelecimentoLogoStorageService(@Value("${bravvo.storage.logos-dir:/app/storage/logos}") String logosDir) {
		this.logosDir = Paths.get(logosDir).toAbsolutePath().normalize();
	}

	/**
	 * Monta a resposta HTTP do logo (bytes + content-type + cache).
	 * 
	 * @param logoKey   nome/arquivo salvo no disco (ex.: "estab_7_logo.png")
	 * @param mimeType  mime type salvo no banco (ex.: "image/png")
	 * @param updatedAt timestamp de atualização do logo (para ETag/controle
	 *                  simples)
	 */
	public ResponseEntity<byte[]> buildLogoResponse(String logoKey, String mimeType, LocalDateTime updatedAt) {
		if (logoKey == null || logoKey.isBlank()) {
			throw new NotFoundException("Logo não encontrada");
		}

		Path filePath = logosDir.resolve(logoKey).normalize();

		// Segurança: garante que não escapou do diretório base
		if (!filePath.startsWith(logosDir)) {
			throw new NotFoundException("Logo não encontrada");
		}

		if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
			throw new NotFoundException("Logo não encontrada");
		}

		try {
			byte[] bytes = Files.readAllBytes(filePath);

			MediaType mediaType = parseMediaType(mimeType);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(mediaType);

			// Cache público, mas com cache-busting via query param ?v=
			headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());

			// ETag simples (não perfeito, mas ok pro MVP)
			if (updatedAt != null) {
				headers.setETag("\"logo-" + updatedAt.toString() + "\"");
			}

			return ResponseEntity.ok().headers(headers).body(bytes);

		} catch (Exception e) {
			throw new NotFoundException("Logo não encontrada");
		}
	}

	private MediaType parseMediaType(String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(mimeType);
		} catch (Exception ignored) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}