package org.jipc.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.ReadableByteChannel;

public class ReadableFileIpcChannel extends AbstractFileIpcChannel implements
		ReadableByteChannel, InterruptibleChannel {


	public ReadableFileIpcChannel(File file) throws IOException {
		super(file, "r");
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		int count = getFileChannel().read(dst);
		if (count == -1 && !hasCloseMarker()) {
			return 0;
		}
		return count;
	}

}
