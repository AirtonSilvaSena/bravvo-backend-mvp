package br.com.bravvo.api.util;

import java.security.SecureRandom;

public class VerificationCodeUtils {

	private static final SecureRandom RND = new SecureRandom();

	private VerificationCodeUtils() {
	}

	public static String generate6Digits() {
		int n = 100000 + RND.nextInt(900000);
		return String.valueOf(n);
	}
}
