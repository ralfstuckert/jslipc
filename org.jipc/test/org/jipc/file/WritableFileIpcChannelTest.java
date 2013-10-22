package org.jipc.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jipc.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WritableFileIpcChannelTest {

	final static int SIZE = 100;

	private File file;
	private File closedMarker;

	private ByteBuffer buffer;

	@Before
	public void setUp() throws Exception {
		file = File.createTempFile("test", ".txt");
		closedMarker = new File(file.getAbsolutePath() + ".closed");
		buffer = TestUtil.createByteBuffer(SIZE);
	}

	@After
	public void tearDown() throws Exception {
		if (closedMarker.exists()) {
			assertTrue(closedMarker.delete());
		}
	}

	@SuppressWarnings("resource")
	@Test(expected = NullPointerException.class)
	public void testWritableFileIpcChannel() throws Exception {
		new WritableFileIpcChannel(null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testWrite() throws Exception {
		WritableFileIpcChannel channel = new WritableFileIpcChannel(file);

		assertEquals(7, channel.write(toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(0, channel.write(toBuffer("")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(4, channel.write(toBuffer("karl")));
		TestUtil.assertEquals("herbertkarl", file);
	}

	@SuppressWarnings("resource")
	@Test
	public void testEndOfStream() throws Exception {
		WritableFileIpcChannel channel = new WritableFileIpcChannel(file);

		assertEquals(7, channel.write(toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		// simulate close by writer
		assertTrue(closedMarker.createNewFile());
		assertEquals(0, channel.write(toBuffer("karl")));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		WritableFileIpcChannel channel = new WritableFileIpcChannel(file);
		channel.close();
		channel.write(buffer);
	}

	@Test
	public void testClose() throws Exception {
		assertFalse(closedMarker.exists());
		WritableFileIpcChannel channel = new WritableFileIpcChannel(file);
		channel.close();
		assertTrue(closedMarker.exists());
	}

	@Test
	public void testIsOpen() throws Exception {
		WritableFileIpcChannel channel = new WritableFileIpcChannel(file);
		assertEquals(true, channel.isOpen());

		channel.close();
		assertEquals(false, channel.isOpen());
	}

	private ByteBuffer toBuffer(final String text) {
		return ByteBuffer.wrap(text.getBytes());
	}
	
}
