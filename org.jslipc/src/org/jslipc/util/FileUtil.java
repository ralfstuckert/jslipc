package org.jslipc.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some file helper methods.
 */
public class FileUtil {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(FileUtil.class);
	
	/**
	 * Deletes a file or directory.
	 * 
	 * @param file
	 */
	public static void delete(final File file) {
		delete(file, true);
	}

	/**
	 * Deletes a file or directory.
	 * 
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

		if (!file.delete()) {
			LOGGER.warn("failed to delete {}, trying delete on exit", file);
			file.deleteOnExit();
		}
	}

	/**
	 * Creates a new directory with a unique name in the given directory.
	 * 
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

	/**
	 * Creates a new file with a unique name in the given directory.
	 * 
	 * @param parent
	 * @return the created file
	 */
	public static File createFile(final File parent) throws IOException {
		File file = null;
		do {
			String name = UUID.randomUUID().toString();
			file = new File(parent, name);
		} while (!file.createNewFile());
		return file;
	}

}
