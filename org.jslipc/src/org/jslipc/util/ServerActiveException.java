package org.jslipc.util;

import java.io.File;
import java.io.IOException;

public class ServerActiveException extends IOException {

	final private File activeServerDirectory;

	public ServerActiveException(File activeServerDirectory) {
		super("directory " + activeServerDirectory.getAbsolutePath() + " is currently used by another server");
		this.activeServerDirectory = activeServerDirectory;
	}

	public File getActiveServerDirectory() {
		return activeServerDirectory;
	}
	
	
}
