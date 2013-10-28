package org.jipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptibleChannel;

import org.jipc.channel.WritableJipcByteChannel;

/**
 * A {@link WritableJipcByteChannel} implementation that writes the data to an
 * underlying {@link FileChannel}.
 */
public class WritableJipcFileChannel extends AbstractJipcFileChannel implements
		WritableJipcByteChannel, InterruptibleChannel {

	/**
	 * Creates WritableJipcFileChannel based on the given file.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public WritableJipcFileChannel(File file) throws IOException {
		super(file, "rw");
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkClosed();
		if (getState() == JipcChannelState.ClosedByPeer) {
			return 0;
		}
		return getFileChannel().write(src);
	}

}
