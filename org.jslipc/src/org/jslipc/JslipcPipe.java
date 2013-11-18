package org.jslipc;

import java.io.IOException;

import org.jslipc.channel.ReadableJslipcByteChannel;
import org.jslipc.channel.WritableJslipcByteChannel;

/**
 * Defines the common methods of pipes.
 */
public interface JslipcPipe {

	/**
	 * @return the channel to read from.
	 * @throws IOException 
	 */
	ReadableJslipcByteChannel source() throws IOException;

	/**
	 * @return the channel to write to.
	 * @throws IOException 
	 */
	WritableJslipcByteChannel sink() throws IOException;

}
