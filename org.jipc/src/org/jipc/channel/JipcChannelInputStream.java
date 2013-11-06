package org.jipc.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.jipc.channel.JipcChannel.JipcChannelState;

/**
 * This class wraps a {@link ReadableJipcByteChannel} in order to provide a blocking InputStream.
 */
public class JipcChannelInputStream extends InputStream {

	private static final int SLEEP_TIME = 100;

	private ReadableJipcByteChannel channel;
	private ByteBuffer oneByteBuffer = ByteBuffer.wrap(new byte[1]);

	/**
	 * Creates a JipcChannelInputStream based on the underlying channel.
	 * @param channel
	 */
	public JipcChannelInputStream(ReadableJipcByteChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException("parameter channel must not be null");
		}
		this.channel = channel;
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		channel.close();
	}

	@Override
	public int read() throws IOException {
		oneByteBuffer.clear();
		int count = readBlocking(oneByteBuffer);
		if (count == -1) {
			return -1;
		}
		if (count == 1) {
			return (int) oneByteBuffer.get(0);
		}
		throw new IllegalStateException("expected read count of either 1 or -1");
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return readBlocking(ByteBuffer.wrap(b, off, len));
	}

	protected int readBlocking(ByteBuffer buffer) throws IOException,
			InterruptedIOException {
		int bytesRead = 0;
		while ((bytesRead = channel.read(buffer)) == 0) {
			if (channel.getState() == JipcChannelState.ClosedByPeer) {
				return -1;
			}
			sleep();
		}
		return bytesRead;
	}

	protected void sleep() throws InterruptedIOException {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

}
