package org.jipc.channel.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;

import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.ReadableJipcByteChannel;

/**
 * A {@link ReadableJipcByteChannel} implementation that reads the data from an underlying
 * {@link ByteBufferQueue}.
 */
public class ReadableBbqChannel extends AbstractBbqChannel implements
		ReadableJipcByteChannel, InterruptibleChannel {

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
		if (count == 0 && queue.isClosed()) {
			return -1; // end of stream
		}
		return count;
	}

	public InputStream newInputStream() {
		return new JipcChannelInputStream(this);
	}

}
