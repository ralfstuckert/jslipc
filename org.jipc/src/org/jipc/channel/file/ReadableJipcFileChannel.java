package org.jipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptibleChannel;

import org.jipc.channel.ReadableJipcByteChannel;

/**
 * A {@link ReadableJipcByteChannel} implementation that reads the data from an underlying
 * {@link FileChannel}.
 */
public class ReadableJipcFileChannel extends AbstractJipcFileChannel implements
		ReadableJipcByteChannel, InterruptibleChannel {


	/**
	 * Creates ReadableJipcFileChannel based on the given file.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public ReadableJipcFileChannel(File file) throws IOException {
		super(file, "r");
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		int count = getFileChannel().read(dst);
		if (count == -1 && getState() != JipcChannelState.ClosedByPeer) {
			return 0;
		}
		return count;
	}
	


}
