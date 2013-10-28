package org.jipc;

import java.io.Closeable;

/**
 * If method {@link #cleanUpOnClose()} is called prior to {@link #close()}, an implementation
 * of this interface should try to release all resource it relies on (e.g. files, buffers, etc.), 
 * whether create by itself or passed in.
 */
public interface JipcBinman extends Closeable {

	/**
	 * Attempts to clean up any resources on {@link #close()} if they are no longer needed.
	 */
	void cleanUpOnClose();
}
