package org.jipc.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class WritableBbqChannel extends AbstractBbqChannel implements
		WritableByteChannel {

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
		return new WritableBbqChannelOutputStream();
	}

	private class WritableBbqChannelOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			while (!queue.offer((byte) b)) {
				sleep();
			}
		}
	}

}
