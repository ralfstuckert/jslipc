package org.jipc;

import java.io.IOException;

import org.jipc.channel.ReadableJipcByteChannel;
import org.jipc.channel.WritableJipcByteChannel;

/**
 * Defines the common methods of pipes.
 */
public interface JipcPipe {

	/**
	 * @return the channel to read from.
	 * @throws IOException 
	 */
	ReadableJipcByteChannel source() throws IOException;

	/**
	 * @return the channel to write to.
	 * @throws IOException 
	 */
	WritableJipcByteChannel sink() throws IOException;

}
