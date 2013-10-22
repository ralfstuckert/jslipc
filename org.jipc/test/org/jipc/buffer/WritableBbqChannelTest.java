package org.jipc.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.TestUtil;
import org.junit.Test;

public class WritableBbqChannelTest {

	final static int SIZE = 100;

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testWritableBbqChannelWithNull() {
		new WritableBbqChannel(null);
	}

	@Test
	@SuppressWarnings("resource")
	public void testWritableBbqChannel() {
		new WritableBbqChannel(TestUtil.createByteBufferQueue(SIZE));
	}

	@Test
	public void testIsOpen() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();
		assertTrue(channel.isOpen());
		channel.close();
		assertFalse(channel.isOpen());
	}

	@Test
	public void testClose() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();

		assertFalse(queue.isClosed());
		channel.close();
		assertTrue(queue.isClosed());
	}

	public void testIsClosedByPeer() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();

		assertFalse(channel.isClosedByPeer());
		queue.close();
		assertTrue(channel.isClosedByPeer());
		
		channel.close();
		assertFalse(channel.isClosedByPeer());
	}

	@SuppressWarnings("resource")
	@Test
	public void testWrite() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();

		// test writing 10 bytes
		int capacity = queue.getCapacity();
		ByteBuffer src = TestUtil.createByteBuffer(capacity, (byte) 0);
		for (int i = 0; i < 10; i++) {
			src.put((byte) i);
		}
		src.flip();
		assertEquals(10, channel.write(src));
		for (int i = 0; i < 10; i++) {
			assertEquals(new Byte((byte) i), queue.poll());
		}

		// fill queue
		for (int i = 0; i < capacity; i++) {
			assertTrue(queue.offer((byte) i));
		}

		// cannot write into full queue
		src.clear();
		for (int i = 0; i < 10; i++) {
			src.put((byte) i);
		}
		src.flip();
		assertEquals(0, channel.write(src));

		// remove two bytes from queue and try again
		queue.poll();
		queue.poll();
		assertEquals(2, channel.write(src));
	}

	@SuppressWarnings("resource")
	@Test()
	public void testWriteToNonInitializedQueueBlocks() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		final WritableBbqChannel channel = new WritableBbqChannel(
				queue);

		final ByteBuffer src = TestUtil.createByteBuffer(10, (byte) 33);
		src.flip();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					channel.write(src);
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
	public void testWriteToClosedChannel() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();

		int capacity = queue.getCapacity();
		ByteBuffer src = TestUtil.createByteBuffer(capacity, (byte) 0);
		for (int i = 0; i < 10; i++) {
			src.put((byte) i);
		}
		src.flip();
		channel.close();
		channel.write(src);
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteToOutputStream() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();
		
		OutputStream os = channel.newOutputStream();
		for (int i = 0; i < 10; i++) {
			os.write((byte)i);
		}
		for (int i = 0; i < 10; i++) {
			assertEquals(new Byte((byte) i), queue.poll());
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteToOutputStreamMayBlock() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();
		
		// fill queue
		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) i));
		}
		

		// now try blocking write
		final OutputStream os = channel.newOutputStream();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write((byte)17);
				} catch (Exception e) {
					e.printStackTrace();
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		queue.poll();		
		// now the queue is no longer full
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptWritingToOutputStream() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		WritableBbqChannel channel = new WritableBbqChannel(
				queue);
		queue.init();
		
		// fill queue
		for (int i = 0; i < queue.getCapacity(); i++) {
			assertTrue(queue.offer((byte) i));
		}
		

		// now try blocking write
		final OutputStream os = channel.newOutputStream();
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write((byte)17);
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		thread.interrupt();
		thread.join(1000);
		assertFalse(thread.isAlive());
		assertNotNull("expected InterruptedIOException ", caught.get());
		assertEquals(InterruptedIOException.class, caught.get().getClass());
	}


}
