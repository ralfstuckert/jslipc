package org.jslipc.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.ReadableJslipcByteChannel;
import org.jslipc.channel.JslipcChannel.JslipcChannelState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JslipcChannelInputStreamTest {

	@Mock
	private ReadableJslipcByteChannel channelMock;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() {
		channelMock = null;
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testJslipcChannelInputStream() {
		new JslipcChannelInputStream(null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testRead() throws Exception {
		JslipcChannelInputStream is = new JslipcChannelInputStream(channelMock);
		mockReadByte(17);
		assertEquals((byte) 17, is.read());
		mockReadByte(67);
		assertEquals((byte) 67, is.read());
		mockReadByte(167);
		assertEquals((byte) 167, is.read());
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadEndOfStream() throws Exception {
		JslipcChannelInputStream is = new JslipcChannelInputStream(channelMock);
		when(channelMock.getState()).thenReturn(JslipcChannelState.ClosedByPeer);
		mockReadByte(17);
		assertEquals((byte) 17, is.read());
		mockReadByte(67);
		assertEquals((byte) 67, is.read());

		doReturn(-1).when(channelMock).read(any(ByteBuffer.class));
		assertEquals(-1, is.read());
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadMayBlock() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);

		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		final AtomicReference<Integer> byteRead = new AtomicReference<Integer>();
		Thread thread = new Thread() {
			public void run() {
				try {
					byteRead.set(is.read());
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		mockReadByte(17);
		// now the queue is no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
		assertEquals(new Integer(17), byteRead.get());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptBlockedRead() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);

		// now try blocking write
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
	@Test
	public void testReadByteArray() throws Exception {
		JslipcChannelInputStream is = new JslipcChannelInputStream(channelMock);
		byte[] buf = new byte[10];
		mockReadBytes(17, 12, 56);
		assertEquals(3, is.read(buf, 1, 5));
		assertEquals((byte) 17, buf[1]);
		assertEquals((byte) 12, buf[2]);
		assertEquals((byte) 56, buf[3]);

	}

	@SuppressWarnings("resource")
	@Test
	public void testReadByteArrayEndOfStream() throws Exception {
		JslipcChannelInputStream is = new JslipcChannelInputStream(channelMock);
		when(channelMock.getState()).thenReturn(JslipcChannelState.ClosedByPeer);
		byte[] buf = new byte[10];
		mockReadBytes(17, 12);
		assertEquals(2, is.read(buf, 1, 5));
		assertEquals((byte) 17, buf[1]);
		assertEquals((byte) 12, buf[2]);

		when(channelMock.getState()).thenReturn(JslipcChannelState.ClosedByPeer);
		doReturn(-1).when(channelMock).read(any(ByteBuffer.class));
		assertEquals(-1, is.read(buf, 1, 5));
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadByteArrayMayBlock() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);
		final byte[] buf = new byte[10];

		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		final AtomicReference<Integer> byteRead = new AtomicReference<Integer>();
		Thread thread = new Thread() {
			public void run() {
				try {
					byteRead.set(is.read(buf, 1, 5));
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		mockReadBytes(17, 12);
		// now the queue is no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
		assertEquals((byte) 17, buf[1]);
		assertEquals((byte) 12, buf[2]);
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadTimeout() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);
		is.setTimeout(300);
		final byte[] buf = new byte[10];

		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		final AtomicReference<Integer> byteRead = new AtomicReference<Integer>();
		Thread thread = new Thread() {
			public void run() {
				try {
					byteRead.set(is.read(buf, 1, 5));
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNotNull("expected timeout exception", caught.get());
		assertEquals(InterruptedByTimeoutException.class, caught.get()
				.getClass());
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptBlockedReadByteArray() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);
		final byte[] buf = new byte[10];

		// now try blocking write
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					is.read(buf, 1, 5);
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
	
	@Test
	public void testClose() throws Exception {
		final JslipcChannelInputStream is = new JslipcChannelInputStream(
				channelMock);
		is.close();
		verify(channelMock).close();
	}

	protected void mockReadByte(final int date) throws IOException {
		doAnswer(new Answer<Integer>() {
			public Integer answer(InvocationOnMock invocation) {
				ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
				buffer.put((byte) date);
				return 1;
			}
		}).when(channelMock).read(any(ByteBuffer.class));
	}

	protected void mockReadBytes(final int... dates) throws IOException {
		doAnswer(new Answer<Integer>() {
					public Integer answer(InvocationOnMock invocation) {
						ByteBuffer buffer = (ByteBuffer) invocation
								.getArguments()[0];
						for (int date : dates) {
							buffer.put((byte) date);
						}
						return dates.length;
					}
				}).when(channelMock).read(any(ByteBuffer.class));
	}
}
