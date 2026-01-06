package br.com.bravvo.api.util;

import java.text.Normalizer;

public class SlugUtils {

	private SlugUtils() {
	}

	public static String normalize(String input) {
		if (input == null)
			return null;
		String s = input.trim().toLowerCase();
		s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		s = s.replaceAll("[^a-z0-9-]", "-");
		s = s.replaceAll("-{2,}", "-");
		s = s.replaceAll("^-|-$", "");
		return s;
	}

	public static boolean isValid(String slug) {
		if (slug == null)
			return false;
		return slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$") && slug.length() >= 3 && slug.length() <= 60;
	}
}
