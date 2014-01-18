package org.jslipc.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * A bunch of utility methods for dealing with URLs.
 */
public class UrlUtil {
	
	private UrlUtil() {
		// utility class
	}

	/**
	 * URL encodes the given string.
	 * @param string
	 * @return the encoded string.
	 */
	public static String urlEncode(final String string) {
		try {
			return URLEncoder.encode(string, StringUtil.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 must be supported?!?");
		}
	}
	
	/**
	 * Decodes an URL-encoded string.
	 * @param string
	 * @return the decoded string.
	 */
	public static String urlDecode(final String string) {
		try {
			return URLDecoder.decode(string, StringUtil.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 must be supported?!?");
		}
	}

}
