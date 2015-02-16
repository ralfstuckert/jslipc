package org.jslipc.util;

import java.io.File;
import java.io.IOException;

/**
 * Thrown by {@link HostDir#create(File)} in case there is already an active host.
 * @author Ralf
 *
 */
public class ActiveHostException extends IOException {

	private static final long serialVersionUID = 1L;

	final private File activeHostDirectory;

	public ActiveHostException(File activeHostDirectory) {
		super("directory " + activeHostDirectory.getAbsolutePath() + " is currently used by another host");
		this.activeHostDirectory = activeHostDirectory;
	}

	/**
	 * @return the directory used by the currently active host.
	 */
	public File getActiveHostDirectory() {
		return activeHostDirectory;
	}
	
	
}
