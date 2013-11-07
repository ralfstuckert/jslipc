package org.jipc.channel.file;

import java.io.File;
import java.util.UUID;

/**
 * Some file helper methods.
 */
public class FileUtil {

	/**
	 * Deletes a file or directory.
	 * @param file
	 * @param recursive 
	 */
	public static void delete(final File file, final boolean recursive) {
		if (recursive && file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					delete(child, recursive);
				}
			}
		}
		file.delete();
		file.deleteOnExit();
	}

	/**
	 * Creates a new directory with a unique name in the given directory.
	 * @param parent
	 * @return the created directory
	 */
	public static File createDirectory(final File parent) {
		File file = null;
		do {
			String name = UUID.randomUUID().toString();
			file = new File(parent, name);
		} while (!file.mkdir());
		return file;
	}

	
}
