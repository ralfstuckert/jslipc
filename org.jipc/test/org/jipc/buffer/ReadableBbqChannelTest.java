package org.jipc.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.buffer.ByteBufferQueue;
import org.jipc.buffer.ReadableBbqChannel;
import org.junit.Test;

public class ReadableBbqChannelTest {

	final static int SIZE = 100;

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testReadableByteBufferQueueChannelWithNull() {
		new ReadableBbqChannel(null);
	}

	@Test
	@SuppressWarnings("resource")
	public void testReadableByteBufferQueueChannel() {
		new ReadableBbqChannel(TestUtil.createByteBufferQueue(SIZE));
	}

	@Test
	public void testIsOpen() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		assertTrue(channel.isOpen());
		channel.close();
		assertFalse(channel.isOpen());
	}

	@Test
	public void testClose() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();

		assertFalse(queue.isClosed());
		channel.close();
		assertTrue(queue.isClosed());
	}

	@SuppressWarnings("resource")
	@Test
	public void testRead() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();

		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}

		ByteBuffer dest = TestUtil.createByteBuffer(SIZE, (byte) 0);
		assertEquals(10, channel.read(dest));
		for (int i = 0; i < 10; i++) {
			assertEquals(i, dest.get(i));
		}

		assertEquals(0, channel.read(dest));

		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}
		assertEquals(10, channel.read(dest));
		for (int i = 0; i < 10; i++) {
			assertEquals(i, dest.get(i + 10));
		}

	}

	@SuppressWarnings("resource")
	@Test()
	public void testReadOnNonInitializedQueueBlocks() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		final ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		final ByteBuffer dest = TestUtil.createByteBuffer(SIZE, (byte) 0);
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					channel.read(dest);
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		queue.init();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
	}

	@Test(expected = ClosedChannelException.class)
	public void testReadFromClosedChannel() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();

		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}
		ByteBuffer dest = TestUtil.createByteBuffer(SIZE, (byte) 0);
		channel.close();
		channel.read(dest);
	}

	@Test
	public void testInputStream() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();

		InputStream is = Channels.newInputStream(channel);
		assertEquals(-1, is.read());

		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}

		for (int i = 0; i < 10; i++) {
			assertEquals(i, is.read());
		}
		assertEquals(-1, is.read());

	}
	
	@SuppressWarnings("resource")
	@Test
	public void testReadFromInputStream() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}
		
		InputStream is = channel.newInputStream();
		for (int i = 0; i < 10; i++) {
			assertEquals(i, is.read());
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadFromInputStreamWithQueueClosed() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}
		
		InputStream is = channel.newInputStream();
		for (int i = 0; i < 9; i++) {
			assertEquals(i, is.read());
		}
		
		queue.close();
		assertEquals(9, is.read());
		
		assertEquals(-1, is.read());
		assertEquals(-1, is.read());
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadFromInputStreamMayBlock() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		// now try blocking write
		final InputStream is = channel.newInputStream();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					is.read();
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		assertTrue(queue.offer((byte) 17));
		// now the queue is no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptReadingFromBlockedInputStream() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		// now try blocking write
		final InputStream is = channel.newInputStream();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					is.read();
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertTrue(thread.isAlive());
		assertNull("caught exception " + caught.get(), caught.get());

		thread.interrupt();
		thread.join(1000);
		assertFalse(thread.isAlive());
		assertNotNull("expected InterruptedIOException ", caught.get());
		assertEquals(InterruptedIOException.class, caught.get().getClass());
	}
	
	
}
