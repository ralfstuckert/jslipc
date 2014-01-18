package org.jslipc.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;

import org.jslipc.TimeoutAware;
import org.jslipc.channel.JslipcChannel.JslipcChannelState;
import org.jslipc.util.TimeUtil;

/**
 * This class wraps a {@link ReadableJslipcByteChannel} in order to provide a blocking InputStream.
 */
public class JslipcChannelInputStream extends InputStream implements TimeoutAware {

	private ReadableJslipcByteChannel channel;
	private ByteBuffer oneByteBuffer = ByteBuffer.wrap(new byte[1]);
	private int timeout = 0;

	/**
	 * Creates a JslipcChannelInputStream based on the underlying channel.
	 * @param channel
	 */
	public JslipcChannelInputStream(ReadableJslipcByteChannel channel) {
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
		long waitingSince = System.currentTimeMillis();
		int bytesRead = 0;
		while ((bytesRead = channel.read(buffer)) == 0) {
			if (channel.getState() == JslipcChannelState.ClosedByPeer) {
				return -1;
			}
			sleep(waitingSince);
		}
		return bytesRead;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("parameter timeout must be > 0: " + timeout);
		}
		this.timeout = timeout;
	}

	/**
	 * Sleeps for the default time and watches for timeouts.
	 * @param waitingSince the timestamp when the operation started to block.
	 * @throws InterruptedIOException
	 * @throws InterruptedByTimeoutException
	 */
	protected void sleep(long waitingSince) throws InterruptedIOException {
		try {
			TimeUtil.sleep(getTimeout(), waitingSince);
		} catch (InterruptedException e) {
			throw new InterruptedIOException("interrupted by timeout");
		}
	}
	

}
