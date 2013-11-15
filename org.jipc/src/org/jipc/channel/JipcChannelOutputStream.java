package org.jipc.channel;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;

import org.jipc.TimeoutAware;
import org.jipc.channel.JipcChannel.JipcChannelState;
import org.jipc.util.TimeUtil;

/**
 * This class wraps a {@link WritableJipcByteChannel} in order to provide a blocking OutputStream.
 */
public class JipcChannelOutputStream extends OutputStream implements TimeoutAware {

	private WritableJipcByteChannel channel;
	private ByteBuffer oneByteBuffer = ByteBuffer.wrap(new byte[1]);
	private int timeout = 0;

	/**
	 * Creates a JipcChannelOutputStream based on the underlying channel.
	 * @param channel
	 */
	public JipcChannelOutputStream(final WritableJipcByteChannel channel) {
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
				&& channel.getState() != JipcChannelState.ClosedByPeer) {
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
	 * @throws InterruptedByTimeoutException
	 */
	protected void sleep(long waitingSince) throws InterruptedIOException, InterruptedByTimeoutException {
		TimeUtil.sleep(getTimeout(), waitingSince);
	}
	
}
