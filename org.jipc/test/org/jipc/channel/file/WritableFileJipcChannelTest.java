package org.jipc.channel.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jipc.TestUtil;
import org.junit.Test;

public class WritableFileJipcChannelTest extends AbstractJipcFileChannelTest {

	@Override
	protected WritableJipcFileChannel createChannel(File file) throws IOException {
		return new WritableJipcFileChannel(file);
	}


	@SuppressWarnings("resource")
	@Test(expected = NullPointerException.class)
	public void testWritableFileIpcChannel() throws Exception {
		new WritableJipcFileChannel(null);
	}

	@Test
	public void testWrite() throws Exception {
		WritableJipcFileChannel channel = createChannel(file);

		assertEquals(7, channel.write(toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(0, channel.write(toBuffer("")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(4, channel.write(toBuffer("karl")));
		TestUtil.assertEquals("herbertkarl", file);
	}

	@Test
	public void testEndOfStream() throws Exception {
		WritableJipcFileChannel channel = createChannel(file);

		assertEquals(7, channel.write(toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		// simulate close by writer
		assertTrue(closedMarker.createNewFile());
		assertEquals(0, channel.write(toBuffer("karl")));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		WritableJipcFileChannel channel = createChannel(file);
		channel.close();
		channel.write(buffer);
	}

	private ByteBuffer toBuffer(final String text) {
		return ByteBuffer.wrap(text.getBytes());
	}
	
}
