package com.br.real_estate_platform.service;

import java.util.regex.Pattern;

public final class PhotoNaming {

	public static final String PUBLIC_PATH = "/api/photos/";

	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	public static final String FILENAME_PATTERN = "^" + UUID_PATTERN + "\\.(jpg|png|webp)$";

	public static final String URL_PATTERN = "^" + PUBLIC_PATH + UUID_PATTERN + "\\.(jpg|png|webp)$";

	private static final Pattern FILENAME = Pattern.compile(FILENAME_PATTERN);

	private PhotoNaming() {
	}

	public static boolean isValidFilename(String filename) {
		return filename != null && FILENAME.matcher(filename).matches();
	}

	public static String urlFor(String filename) {
		return PUBLIC_PATH + filename;
	}
}
