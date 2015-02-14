package org.jslipc.util;

import java.io.File;
import java.io.IOException;

/**
 * Thrown by {@link ServerDir#create(File)} in case there is already an active server.
 * @author Ralf
 *
 */
public class ServerActiveException extends IOException {

	private static final long serialVersionUID = 1L;

	final private File activeServerDirectory;

	public ServerActiveException(File activeServerDirectory) {
		super("directory " + activeServerDirectory.getAbsolutePath() + " is currently used by another server");
		this.activeServerDirectory = activeServerDirectory;
	}

	/**
	 * @return the directory used by the currently active server.
	 */
	public File getActiveServerDirectory() {
		return activeServerDirectory;
	}
	
	
}
