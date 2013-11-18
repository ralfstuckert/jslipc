package org.jslipc.channel.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jslipc.TestUtil;
import org.jslipc.channel.JslipcChannel.JslipcChannelState;
import org.jslipc.channel.buffer.AbstractBbqChannel;
import org.jslipc.channel.buffer.ByteBufferQueue;
import org.junit.Test;

public abstract class AbstractBbqChannelTest {

	final static int SIZE = 100;

	protected abstract AbstractBbqChannel createAbstractBbqChannel(
			final ByteBufferQueue queue) throws Exception;

	@Test
	public void testIsOpen() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		AbstractBbqChannel channel = createAbstractBbqChannel(queue);
		queue.init();
		assertTrue(channel.isOpen());
		channel.close();
		assertFalse(channel.isOpen());
	}

	@Test
	public void testClose() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		AbstractBbqChannel channel = createAbstractBbqChannel(queue);
		queue.init();

		assertFalse(queue.isClosed());
		channel.close();
		assertTrue(queue.isClosed());
	}

	@Test
	public void testGetState() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		AbstractBbqChannel channel = createAbstractBbqChannel(queue);
		queue.init();

		assertEquals(JslipcChannelState.Open, channel.getState());
		queue.close();
		assertEquals(JslipcChannelState.ClosedByPeer, channel.getState());

		channel.close();
		assertEquals(JslipcChannelState.Closed, channel.getState());
	}

}
