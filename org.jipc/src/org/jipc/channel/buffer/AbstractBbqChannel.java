package org.jipc.channel.buffer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;

import org.jipc.channel.JipcChannel;

/**
 * Common base class for {@link ByteBufferQueue} based channels.
 */
public abstract class AbstractBbqChannel implements JipcChannel, InterruptibleChannel {
	private static final int SLEEP_TIME = 100;
	protected volatile ByteBufferQueue queue;
	protected volatile boolean closed;

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
	public JipcChannelState getState() {
		if (!isOpen()) {
			return JipcChannelState.Closed;
		}
		if (queue.isInitialized() && queue.isClosed()) {
			return JipcChannelState.ClosedByPeer;
		}
		return JipcChannelState.Open;
	}


	protected void checkClosed() throws ClosedChannelException {
		if (!isOpen()) {
			throw new ClosedChannelException();
		}
	}

	protected void waitForInitialization() throws InterruptedIOException {
		while (!queue.isInitialized()) {
			sleep();
		}
	}

	protected void sleep() throws InterruptedIOException {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			throw new InterruptedIOException(e.getMessage());
		}
	}

	protected void waitForNonEmpty() throws InterruptedIOException {
		while (queue.isEmpty()) {
			sleep();
		}
	}

	protected void waitForNonFull() throws InterruptedIOException {
		while (queue.isFull()) {
			sleep();
		}
	}

}
