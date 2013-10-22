package org.jipc;

/**
 * Defines the common methods of pipes.
 */
public interface JipcPipe {

	/**
	 * @return the channel to read from.
	 */
	ReadableJipcByteChannel source();

	/**
	 * @return the channel to write to.
	 */
	WritableJipcByteChannel sink();

}
