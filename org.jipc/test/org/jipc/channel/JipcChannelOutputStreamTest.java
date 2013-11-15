package org.jipc.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.channel.JipcChannel.JipcChannelState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JipcChannelOutputStreamTest {

	@Mock
	private WritableJipcByteChannel channelMock;

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
	public void testJipcChannelOutputStream() {
		new JipcChannelOutputStream(null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testWrite() throws Exception {
		JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);
		mockWriteByte(17);
		os.write(17);
		verifyWriteCalled();

		mockWriteByte(67);
		os.write(67);
		verifyWriteCalled();

		mockWriteByte(167);
		os.write(167);
		verifyWriteCalled();
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteEndOfStream() throws Exception {
		JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);
		when(channelMock.getState()).thenReturn(JipcChannelState.ClosedByPeer);
		mockWriteByte(17);
		os.write(17);
		verifyWriteCalled();

		mockWriteByte(67);
		os.write(67);
		verifyWriteCalled();

		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		when(channelMock.getState()).thenReturn(JipcChannelState.ClosedByPeer);
		try {
			os.write((byte) 78);
			fail("expected exception");
		} catch (ClosedChannelException e) {
			// expected
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteMayBlock() throws Exception {
		final JipcChannelOutputStream os = new JipcChannelOutputStream(
				channelMock);

		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write(17);
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		mockWriteByte(17);
		// now the queue os no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptBlockedWrite() throws Exception {
		final JipcChannelOutputStream os = new JipcChannelOutputStream(
				channelMock);

		// now try blocking write
		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write(17);
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
	public void testWriteByteArray() throws Exception {
		JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);

		mockWriteBytes(17, 12, 56);
		byte[] buf = createByteArray(10, 1, 17, 12, 56);
		os.write(buf, 1, 3);
		verifyWriteCalled();

		mockWriteBytes(34, 89);
		buf = createByteArray(10, 1, 34, 89);
		os.write(buf, 1, 2);
		verifyWriteCalled();
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteByteArrayEndOfStream() throws Exception {
		JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);
		when(channelMock.getState()).thenReturn(JipcChannelState.ClosedByPeer);

		mockWriteBytes(17, 12, 56);
		byte[] buf = createByteArray(10, 1, 17, 12, 56);
		os.write(buf, 1, 3);
		verifyWriteCalled();

		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		when(channelMock.getState()).thenReturn(JipcChannelState.ClosedByPeer);
		try {
			os.write(buf, 1, 3);
			fail("expected exception");
		} catch (ClosedChannelException e) {
			// expected
		}
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteByteArrayMayBlock() throws Exception {
		final JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);

		final byte[] buf = createByteArray(10, 1, 17, 12, 56);
		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write(buf, 1, 3);
				} catch (Exception e) {
					caught.set(e);
				}
			}
		};
		thread.start();
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertTrue(thread.isAlive());

		mockWriteBytes(17, 12, 56);
		// now the queue os no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("resource")
	@Test
	public void testWriteTimeout() throws Exception {
		final JipcChannelOutputStream os = new JipcChannelOutputStream(channelMock);
		os.setTimeout(300);
		final byte[] buf = createByteArray(10, 1, 17, 12, 56);
		doReturn(0).when(channelMock).write(any(ByteBuffer.class));
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write(buf, 1, 3);
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
	public void testInterruptBlockedwriteByteArray() throws Exception {
		final JipcChannelOutputStream os = new JipcChannelOutputStream(
				channelMock);
		final byte[] buf = new byte[10];

		// now try blocking write
		final AtomicReference<Exception> caught = new AtomicReference<Exception>();
		Thread thread = new Thread() {
			public void run() {
				try {
					os.write(buf, 1, 5);
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
		final JipcChannelOutputStream is = new JipcChannelOutputStream(
				channelMock);
		is.close();
		verify(channelMock).close();
	}


	protected void mockWriteByte(final int date) throws IOException {
		doAnswer(new Answer<Integer>() {
			public Integer answer(InvocationOnMock invocation) {
				ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
				assertEquals(1, buffer.remaining());
				assertEquals((byte) date, buffer.get());
				return 1;
			}
		}).when(channelMock).write(any(ByteBuffer.class));
	}

	protected void mockWriteBytes(final int... dates) throws IOException {
		doAnswer(new Answer<Integer>() {
			public Integer answer(InvocationOnMock invocation) {
				ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
				assertEquals(dates.length, buffer.remaining());
				for (int date : dates) {
					assertEquals((byte) date, buffer.get());
				}
				return dates.length;
			}
		}).when(channelMock).write(any(ByteBuffer.class));
	}

	protected void verifyWriteCalled() throws IOException {
		verify(channelMock).write(any(ByteBuffer.class));
		reset(channelMock);
	}

	protected byte[] createByteArray(int size, int offset, int... data) {
		byte[] result = new byte[size];
		for (int i = 0; i < data.length; i++) {
			result[i + offset] = (byte) data[i];
		}
		return result;
	}

}
