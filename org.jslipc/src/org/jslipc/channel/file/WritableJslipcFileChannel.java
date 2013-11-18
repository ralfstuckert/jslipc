package org.jslipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptibleChannel;

import org.jslipc.channel.WritableJslipcByteChannel;

/**
 * A {@link WritableJslipcByteChannel} implementation that writes the data to an
 * underlying {@link FileChannel}.
 */
public class WritableJslipcFileChannel extends AbstractJslipcFileChannel implements
		WritableJslipcByteChannel, InterruptibleChannel {

	/**
	 * Creates WritableJslipcFileChannel based on the given file.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public WritableJslipcFileChannel(File file) throws IOException {
		super(file, "rw");
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkClosed();
		if (getState() == JslipcChannelState.ClosedByPeer) {
			return 0;
		}
		return getFileChannel().write(src);
	}

}
