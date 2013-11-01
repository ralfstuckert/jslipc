package org.jipc.channel.file;

import java.io.File;

/**
 * Some file helper methods.
 */
public class FileHelper {

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
}
