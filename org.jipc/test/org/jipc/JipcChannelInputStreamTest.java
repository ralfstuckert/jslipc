package org.jipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JipcChannelInputStreamTest {

	@Mock
	private ReadableJipcByteChannel channelMock;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testJipcChannelInputStream() {
		new JipcChannelInputStream(null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testRead() throws Exception {
		JipcChannelInputStream is = new JipcChannelInputStream(channelMock);
		mockRead((byte)17);
		assertEquals((byte)17, is.read());
		mockRead((byte)67);
		assertEquals((byte)67, is.read());
		mockRead((byte)167);
		assertEquals((byte)167, is.read());
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadEndOfStream() throws Exception {
		JipcChannelInputStream is = new JipcChannelInputStream(channelMock);
		when(channelMock.isClosedByPeer()).thenReturn(true);
		mockRead((byte)17);
		assertEquals((byte)17, is.read());
		mockRead((byte)67);
		assertEquals((byte)67, is.read());
		
		reset(channelMock);
		when(channelMock.read(any(ByteBuffer.class))).thenReturn(-1);
		assertEquals(-1, is.read());
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadMayBlock() throws Exception {
		final JipcChannelInputStream is = new JipcChannelInputStream(
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

		mockRead((byte)17);
		// now the queue is no longer empty
		thread.join(1000);
		assertNull("caught exception " + caught.get(), caught.get());
		assertFalse(thread.isAlive());
		assertEquals(new Integer(17), byteRead.get());
	}

	@SuppressWarnings("resource")
	@Test
	public void testInterruptBlockedRead() throws Exception {
		final JipcChannelInputStream is = new JipcChannelInputStream(
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
	public void testReadByteArrayIntInt() throws Exception {
		JipcChannelInputStream is = new JipcChannelInputStream(channelMock);
	}
	
	

	protected void mockRead(final byte date) throws IOException {
		reset(channelMock);
		when(channelMock.read(any(ByteBuffer.class))).thenAnswer(
				new Answer<Integer>() {
					public Integer answer(InvocationOnMock invocation) {
						ByteBuffer buffer = (ByteBuffer) invocation
								.getArguments()[0];
						buffer.put((byte) date);
						return 1;
					}
				});
	}

}
