package org.jipc;

import java.io.Closeable;

public interface JipcBinman extends Closeable {

	/**
	 * Attempts to clean up any resources on {@link #close()} if they are no longer needed.
	 */
	void cleanUpOnClose();
}
