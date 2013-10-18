package org.jipc.buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicReference;

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

	@SuppressWarnings("resource")
	@Test
	public void testEndOfStream() throws Exception {
		final ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		for (int i = 0; i < 10; i++) {
			assertTrue(queue.offer((byte) i));
		}
		ByteBuffer dest = TestUtil.createByteBuffer(SIZE, (byte) 0);
		queue.close();
		
		assertEquals(10, channel.read(dest));
		for (int i = 0; i < 10; i++) {
			assertEquals(i, dest.get(i));
		}

		assertEquals(-1, channel.read(dest));
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
	
	@Test
	public void testInputStreamWithQueueClosed() throws Exception {
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
	

	@SuppressWarnings("resource")
	@Test(timeout = 5000)
	public void testReadFromReader() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		String text = "hello\nhow are you?\nI'm fine\n";
		for (char character : text.toCharArray()) {
			assertTrue(queue.offer((byte) character));
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(channel.newInputStream()));
		assertEquals("hello", reader.readLine());
		assertEquals("how are you?", reader.readLine());
		
		queue.close();
		// check reading to end of stream
		assertEquals("I'm fine", reader.readLine());
		// now we are done
		assertEquals(null, reader.readLine());
	}

	@SuppressWarnings("resource")
	@Test(timeout = 5000)
	public void testReadFromReaderMayBlock() throws Exception {
		ByteBufferQueue queue = TestUtil.createByteBufferQueue(SIZE);
		ReadableBbqChannel channel = new ReadableBbqChannel(
				queue);
		queue.init();
		
		String text = "hello\n";
		for (char character : text.toCharArray()) {
			assertTrue(queue.offer((byte) character));
		}
		
		final BufferedReader reader = new BufferedReader(new InputStreamReader(channel.newInputStream()));
		assertEquals("hello", reader.readLine());

		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		final AtomicReference<String> lineRead = new AtomicReference<String>();
		Thread thread = new Thread() {
			public void run() {
				try {
					lineRead.set( reader.readLine());
				} catch (Exception e) {
					e.printStackTrace();
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertNull("read line " + lineRead.get(), lineRead.get());
		assertTrue(thread.isAlive());

		text = "how are you?\n";
		for (char character : text.toCharArray()) {
			assertTrue(queue.offer((byte) character));
		}
		
		// now the queue is no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertEquals("how are you?", lineRead.get());
		assertFalse(thread.isAlive());
	}


}
