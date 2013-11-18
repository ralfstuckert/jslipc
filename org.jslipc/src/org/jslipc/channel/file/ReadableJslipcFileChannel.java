package org.jslipc.channel.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.InterruptibleChannel;

import org.jslipc.channel.ReadableJslipcByteChannel;

/**
 * A {@link ReadableJslipcByteChannel} implementation that reads the data from an underlying
 * {@link FileChannel}.
 */
public class ReadableJslipcFileChannel extends AbstractJslipcFileChannel implements
		ReadableJslipcByteChannel, InterruptibleChannel {


	/**
	 * Creates ReadableJslipcFileChannel based on the given file.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public ReadableJslipcFileChannel(File file) throws IOException {
		super(file, "r");
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		int count = getFileChannel().read(dst);
		if (count == -1 && getState() != JslipcChannelState.ClosedByPeer) {
			return 0;
		}
		return count;
	}
	


}
