package org.jipc.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;

import org.jipc.WritableJipcByteChannel;

public class WritableFileIpcChannel extends AbstractFileIpcChannel implements
		WritableJipcByteChannel, InterruptibleChannel {

	public WritableFileIpcChannel(File file) throws IOException {
		super(file, "rw");
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkClosed();
		if (isClosedByPeer()) {
			return 0;
		}
		return getFileChannel().write(src);
	}

}
