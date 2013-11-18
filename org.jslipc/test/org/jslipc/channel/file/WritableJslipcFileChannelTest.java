package org.jslipc.channel.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jslipc.TestUtil;
import org.jslipc.channel.file.WritableJslipcFileChannel;
import org.junit.Test;

public class WritableJslipcFileChannelTest extends AbstractJslipcFileChannelTest {

	@Override
	protected WritableJslipcFileChannel createChannel(File file) throws IOException {
		return new WritableJslipcFileChannel(file);
	}


	@SuppressWarnings("resource")
	@Test(expected = NullPointerException.class)
	public void testWritableFileIpcChannel() throws Exception {
		new WritableJslipcFileChannel(null);
	}

	@Test
	public void testWrite() throws Exception {
		WritableJslipcFileChannel channel = createChannel(file);

		assertEquals(7, channel.write(TestUtil.toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(0, channel.write(TestUtil.toBuffer("")));
		TestUtil.assertEquals("herbert", file);

		assertEquals(4, channel.write(TestUtil.toBuffer("karl")));
		TestUtil.assertEquals("herbertkarl", file);
	}

	@Test
	public void testEndOfStream() throws Exception {
		WritableJslipcFileChannel channel = createChannel(file);

		assertEquals(7, channel.write(TestUtil.toBuffer("herbert")));
		TestUtil.assertEquals("herbert", file);

		// simulate close by reader
		assertTrue(closedMarker.createNewFile());
		assertEquals(0, channel.write(TestUtil.toBuffer("karl")));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		WritableJslipcFileChannel channel = createChannel(file);
		channel.close();
		channel.write(buffer);
	}

}
