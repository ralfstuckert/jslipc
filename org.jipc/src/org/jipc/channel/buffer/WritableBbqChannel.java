package org.jipc.channel.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.WritableJipcByteChannel;

/**
 * A {@link WritableBbqChannel} implementation that writes the data to an underlying
 * {@link ByteBufferQueue}.
 */
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
