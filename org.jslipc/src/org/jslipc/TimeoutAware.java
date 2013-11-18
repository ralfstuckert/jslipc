package org.jslipc;

/**
 * Implemented by classes that support a timeout for blocking operations..
 */
public interface TimeoutAware {

	/**
	 * @return the timeout to wait on blocking operations.
	 */
	public int getTimeout();

	/**
	 * Sets the timeout to wait on blocking operations.
	 * Must be >= 0. A value of 0 means wait infinitely.
	 * @param timeout
	 */
	public void setTimeout(int timeout);

}
