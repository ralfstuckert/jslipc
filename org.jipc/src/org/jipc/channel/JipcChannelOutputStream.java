package org.jipc.channel;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

public class JipcChannelOutputStream extends OutputStream {

	private static final int SLEEP_TIME = 100;

	private WritableJipcByteChannel channel;
	private ByteBuffer oneByteBuffer = ByteBuffer.wrap(new byte[1]);

	public JipcChannelOutputStream(final WritableJipcByteChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException(
					"parameter 'channel' must not be  null");
		}
		this.channel = channel;
	}

	@Override
	public void write(int b) throws IOException {
		oneByteBuffer.clear();
		oneByteBuffer.put((byte) b);
		oneByteBuffer.flip();
		writeBlocking(oneByteBuffer);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
		writeBlocking(buffer);
	}

	protected void writeBlocking(ByteBuffer buffer) throws IOException,
			InterruptedIOException, ClosedChannelException {
		int count = 0;
		while ((count = channel.write(buffer)) == 0
				&& !channel.isClosedByPeer()) {
			sleep();
		}
		if (count == 0) {
			throw new ClosedChannelException();
		}
	}

	protected void sleep() throws InterruptedIOException {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

}
