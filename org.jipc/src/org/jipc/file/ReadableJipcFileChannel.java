package org.jipc.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;

import org.jipc.ReadableJipcByteChannel;

public class ReadableJipcFileChannel extends AbstractJipcFileChannel implements
		ReadableJipcByteChannel, InterruptibleChannel {


	public ReadableJipcFileChannel(File file) throws IOException {
		super(file, "r");
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		int count = getFileChannel().read(dst);
		if (count == -1 && !isClosedByPeer()) {
			return 0;
		}
		return count;
	}
	


}
