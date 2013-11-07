package org.jipc.channel.file.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import org.jipc.TestUtil;
import org.junit.Test;

public class WritableChunkFileChannelTest extends AbstractChunkFileChannelTest {

	@Override
	protected WritableChunkFileChannel createChannel(File directory) {
		return new WritableChunkFileChannel(directory);
	}

	@Test
	public void testWrite() throws Exception {
		WritableChunkFileChannel channel = createChannel(directory);

		assertEquals(7, channel.write(TestUtil.toBuffer("herbert")));
		assertChunkExists(0, "herbert");

		assertEquals(0, channel.write(TestUtil.toBuffer("")));
		assertChunkExists(1, "");

		assertEquals(4, channel.write(TestUtil.toBuffer("karl")));
		assertChunkExists(2, "karl");
	}
	
	@Test
	public void testEndOfStream() throws Exception {
		WritableChunkFileChannel channel = createChannel(directory);

		assertEquals(7, channel.write(TestUtil.toBuffer("herbert")));
		assertChunkExists(0, "herbert");

		// simulate close by reader
		assertTrue(closedMarker.createNewFile());
		assertEquals(0, channel.write(TestUtil.toBuffer("karl")));
		assertChunkNotExists(1, "");
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		WritableChunkFileChannel channel = createChannel(directory);
		channel.close();
		channel.write(buffer);
	}



	private void assertChunkExists(int index, final String content)
			throws IOException {
		File file = new File(directory,
				AbstractChunkFileChannel.CHUNK_FILE_NAME + "_" + index);
		assertTrue("expected " + file + " to exist", file.exists());
		TestUtil.assertEquals(content, file);
	}

	private void assertChunkNotExists(int index, final String content)
			throws IOException {
		File file = new File(directory,
				AbstractChunkFileChannel.CHUNK_FILE_NAME + "_" + index);
		assertFalse("expected " + file + " not to exist", file.exists());
	}

}
