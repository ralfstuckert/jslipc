package org.jslipc.util;

import java.io.File;
import java.io.IOException;

import org.jslipc.ipc.pipe.JslipcPipeServer;

public class PipeUtil {

	public static final String PIPES_DIR_NAME = "pipes";
	public static final String CONNECT_DIR_NAME = "connect";

	/**
	 * Creates a directory named <code>connect</code> in the given host
	 * directory.
	 * 
	 * @param hostDir
	 *            the directory hosting the connect and pipes dir.
	 * @return the created connect directory.
	 * @throws IOException
	 *             if creating the directory failed.
	 */
	public static File createConnectDir(final HostDir hostDir)
			throws IOException {
		if (!hostDir.isActive()) {
			throw new IOException("HostDir is already closed");
		}
		File connectDir = new File(hostDir.getDirectory(), CONNECT_DIR_NAME);
		if (!connectDir.mkdir()) {
			throw new IOException("Failed to create connect dir "
					+ connectDir.getAbsolutePath());
		}
		return connectDir;
	}

	/**
	 * Creates a directory named <code>pipes</code> in the given host directory.
	 * 
	 * @param hostDir
	 *            the directory hosting the connect and pipes dir.
	 * @return the created pipes directory.
	 * @throws IOException
	 *             if creating the directory failed.
	 */
	public static File createPipesDir(final HostDir hostDir) throws IOException {
		if (!hostDir.isActive()) {
			throw new IOException("HostDir is already closed");
		}
		File pipeDir = new File(hostDir.getDirectory(), PIPES_DIR_NAME);
		if (!pipeDir.mkdir()) {
			throw new IOException("Failed to create pipes dir "
					+ pipeDir.getAbsolutePath());
		}
		return pipeDir;
	}


	/**
	 * Returns the connect directory of a {@link JslipcPipeServer} using the {@link HostDir#getActive(File) active host}.
	 * @param hostParentDir the host parent directory.
	 * @return the connect directory of the active host if there is one.
	 * @throws IOException if there is no active host or connect directory.
	 */
	public static File getActiveHostConnectDir(final File hostParentDir) throws IOException {
		File activeHostDir = HostDir.getActive(hostParentDir);
		if (activeHostDir == null) {
			throw new IOException("no active host dir found in " + hostParentDir);
		}
		File connectDir = new File(activeHostDir, CONNECT_DIR_NAME);
		if (!connectDir.isDirectory()) {
			throw new IOException("connect dir is not a directory: "
					+ connectDir.getAbsolutePath());
		}
		return connectDir;
	}
}
