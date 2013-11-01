package org.jipc.channel.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jipc.TestUtil;
import org.junit.Test;

public class ReadableJipcFileChannelTest extends AbstractJipcFileChannelTest {

	@Override
	protected ReadableJipcFileChannel createChannel(File file) throws IOException {
		return new ReadableJipcFileChannel(file);
	}

	@SuppressWarnings("resource")
	@Test(expected = NullPointerException.class)
	public void testReadableFileIpcChannel() throws Exception {
		new ReadableJipcFileChannel(null);
	}

	@Test
	public void testRead() throws Exception {
		ReadableJipcFileChannel channel = createChannel(file);
		assertEquals(0, channel.read(buffer));

		TestUtil.appendToFile(file, "herbert");
		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
		TestUtil.assertEquals("", buffer);

		TestUtil.appendToFile(file, "karl");
		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);
	}

	@Test
	public void testEndOfStream() throws Exception {
		ReadableJipcFileChannel channel = createChannel(file);
		assertEquals(0, channel.read(buffer));

		TestUtil.appendToFile(file, "herbert");
		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
		TestUtil.assertEquals("", buffer);

		TestUtil.appendToFile(file, "karl");
		// simulate close by writer
		assertTrue(closedMarker.createNewFile());

		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);
		assertEquals(-1, channel.read(buffer));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		ReadableJipcFileChannel channel = createChannel(file);
		channel.close();
		channel.read(buffer);
	}

}
