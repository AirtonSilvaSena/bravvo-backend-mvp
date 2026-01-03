package br.com.bravvo.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class TokenHashUtils {

	private TokenHashUtils() {
	}

	/**
	 * Gera hash SHA-256 de uma string (usado para refresh token).
	 */
	public static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao gerar hash SHA-256 do token.", e);
		}
	}
}
