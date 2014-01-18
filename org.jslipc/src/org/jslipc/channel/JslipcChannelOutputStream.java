package org.jslipc.channel;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jslipc.TimeoutAware;
import org.jslipc.channel.JslipcChannel.JslipcChannelState;
import org.jslipc.util.TimeUtil;

/**
 * This class wraps a {@link WritableJslipcByteChannel} in order to provide a blocking OutputStream.
 */
public class JslipcChannelOutputStream extends OutputStream implements TimeoutAware {

	private WritableJslipcByteChannel channel;
	private ByteBuffer oneByteBuffer = ByteBuffer.wrap(new byte[1]);
	private int timeout = 0;

	/**
	 * Creates a JslipcChannelOutputStream based on the underlying channel.
	 * @param channel
	 */
	public JslipcChannelOutputStream(final WritableJslipcByteChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException(
					"parameter 'channel' must not be  null");
		}
		this.channel = channel;
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		channel.close();
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
		long waitingSince = System.currentTimeMillis();
		int count = 0;
		while ((count = channel.write(buffer)) == 0
				&& channel.getState() != JslipcChannelState.ClosedByPeer) {
			sleep(waitingSince);
		}
		if (count == 0) {
			throw new ClosedChannelException();
		}
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
	 */
	protected void sleep(long waitingSince) throws InterruptedIOException {
		try {
			TimeUtil.sleep(getTimeout(), waitingSince);
		} catch (InterruptedException e) {
			throw new InterruptedIOException("interrupted by timeout");
		}
	}
	
}
