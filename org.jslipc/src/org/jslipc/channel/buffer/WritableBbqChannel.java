package org.jslipc.channel.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.channel.WritableJslipcByteChannel;

/**
 * A {@link WritableBbqChannel} implementation that writes the data to an underlying
 * {@link ByteBufferQueue}.
 */
public class WritableBbqChannel extends AbstractBbqChannel implements
		WritableJslipcByteChannel {

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
		return new JslipcChannelOutputStream(this);
	}

}
