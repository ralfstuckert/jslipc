package org.jslipc.channel.buffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;

import org.jslipc.TimeoutAware;
import org.jslipc.channel.JslipcChannel;
import org.jslipc.util.TimeUtil;

/**
 * Common base class for {@link ByteBufferQueue} based channels.
 */
public abstract class AbstractBbqChannel implements JslipcChannel, InterruptibleChannel, TimeoutAware {

	protected volatile ByteBufferQueue queue;
	protected volatile boolean closed;
	private int timeout = 0;


	public AbstractBbqChannel(final ByteBufferQueue queue) {
		if (queue == null) {
			throw new IllegalArgumentException(
					"parameter queue must not be null");
		}
		this.queue = queue;
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}
	
	@Override
	public void close() throws IOException {
		this.closed = true;
		if (queue.isInitialized()) {
			queue.close();
		}
	}

	@Override
	public JslipcChannelState getState() {
		if (!isOpen()) {
			return JslipcChannelState.Closed;
		}
		if (queue.isInitialized() && queue.isClosed()) {
			return JslipcChannelState.ClosedByPeer;
		}
		return JslipcChannelState.Open;
	}


	protected void checkClosed() throws ClosedChannelException {
		if (!isOpen()) {
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
	protected void sleep(long waitingSince) throws InterruptedIOException {
		try {
			TimeUtil.sleep(getTimeout(), waitingSince);
		} catch (InterruptedException e) {
			throw new InterruptedIOException("interrupted by timeout");
		}
	}
	
	protected void waitForInitialization() throws InterruptedIOException {
		long waitingSince = System.currentTimeMillis();
		while (!queue.isInitialized()) {
			sleep(waitingSince);
		}
	}

	protected void waitForNonEmpty() throws InterruptedIOException {
		long waitingSince = System.currentTimeMillis();
		while (queue.isEmpty()) {
			sleep(waitingSince);
		}
	}

	protected void waitForNonFull() throws InterruptedIOException {
		long waitingSince = System.currentTimeMillis();
		while (queue.isFull()) {
			sleep(waitingSince);
		}
	}

}
