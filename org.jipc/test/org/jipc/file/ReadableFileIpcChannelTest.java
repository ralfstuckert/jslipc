package org.jipc.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jipc.buffer.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReadableFileIpcChannelTest {

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
	public void testReadableFileIpcChannel() throws Exception {
		new ReadableFileIpcChannel(null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testRead() throws Exception {
		ReadableFileIpcChannel channel = new ReadableFileIpcChannel(file);
		assertEquals(0, channel.read(buffer));

		TestUtil.writeToFile(file, "herbert");
		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
		TestUtil.assertEquals("", buffer);

		TestUtil.writeToFile(file, "karl");
		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);
	}

	@SuppressWarnings("resource")
	@Test
	public void testEndOfStream() throws Exception {
		ReadableFileIpcChannel channel = new ReadableFileIpcChannel(file);
		assertEquals(0, channel.read(buffer));

		TestUtil.writeToFile(file, "herbert");
		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
		TestUtil.assertEquals("", buffer);

		TestUtil.writeToFile(file, "karl");
		// simulate close by writer
		assertTrue(closedMarker.createNewFile());

		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);
		assertEquals(-1, channel.read(buffer));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		ReadableFileIpcChannel channel = new ReadableFileIpcChannel(file);
		channel.close();
		channel.read(buffer);
	}

	@Test
	public void testClose() throws Exception {
		assertFalse(closedMarker.exists());
		ReadableFileIpcChannel channel = new ReadableFileIpcChannel(file);
		channel.close();
		assertTrue(closedMarker.exists());
	}

	@Test
	public void testIsOpen() throws Exception {
		ReadableFileIpcChannel channel = new ReadableFileIpcChannel(file);
		assertEquals(true, channel.isOpen());

		channel.close();
		assertEquals(false, channel.isOpen());
	}

}
