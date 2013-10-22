package org.jipc.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jipc.JipcChannelOutputStream;
import org.jipc.WritableJipcByteChannel;

public class WritableBbqChannel extends AbstractBbqChannel implements
		WritableJipcByteChannel {

	public WritableBbqChannel(final ByteBufferQueue queue) {
		super(queue);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		checkClosed();
		waitForInitialization();
		int count = 0;
		while (!queue.isFull() && src.hasRemaining()) {
			checkClosed();
			queue.offer(src.get());
			++count;
		}
		return count;
	}

	public OutputStream newOutputStream() {
		return new JipcChannelOutputStream(this);
	}

}
