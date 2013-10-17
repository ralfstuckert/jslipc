package org.jipc.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.ReadableByteChannel;

public class ReadableBbqChannel extends
		AbstractBbqChannel implements ReadableByteChannel,
		InterruptibleChannel {

	public ReadableBbqChannel(final ByteBufferQueue queue) {
		super(queue);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		checkClosed();
		waitForInitialization();
		int count = 0;
		while (dst.hasRemaining() && queue.peek() != null) {
			checkClosed();
			Byte date = queue.poll();
			dst.put(date);
			++count;
		}
		return count;
	}
	
	public InputStream newInputStream() {
		return new ReadableBbqChannelInputStream();
	}
	
	private class ReadableBbqChannelInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			Byte date = null;
			while ((date = queue.poll()) == null) {
				if (queue.isClosed()) {
					return -1;
				}
				sleep();
			}
			return date.intValue();
		}
	}

}
