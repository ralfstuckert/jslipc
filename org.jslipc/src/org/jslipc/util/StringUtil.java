package org.jslipc.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A bunch of utility methods for dealing with URLs.
 */
public class StringUtil {

	private StringUtil() {
		// utility class
	}
	
	/**
	 * Effectively calls {@link #split(String, char) split(text, '\n')}.
	 * @param text
	 * @return the split  string.
	 */
	public static List<String> splitLines(final String text) {
		return split(text, '\n');
	}

	/**
	 * Splits the given text by the given delimiter.
	 * @param text
	 * @param delimiter
	 * @return the split  string.
	 */
	public static List<String> split(final String text, final char delimiter) {
		List<String> result = new ArrayList<String>();
		int lastLineBreak = 0;
		int index = 0;
		while ((index = text.indexOf(delimiter, lastLineBreak)) != -1) {
			result.add(text.substring(lastLineBreak, index));
			lastLineBreak = index + 1;
		}
		if (lastLineBreak < text.length() - 1) {
			result.add(text.substring(lastLineBreak));
		}
		return result;
	}

	/**
	 * Effectively calls {@link #join(char, String...) join('\n', parts)}.
	 * @param parts
	 * @return the joined string.
	 */
	public static String join(final String...parts) {
		return join(',', parts);
	}

	/**
	 * Joins the given Strings with the delimiter.
	 * @param delimiter
	 * @param parts
	 * @return the joined string.
	 */
	public static String join(final char delimiter, final String... parts) {
		StringBuilder bob = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				bob.append(delimiter);
			}
			bob.append(parts[i]);
		}
		return bob.toString();
	}


}
