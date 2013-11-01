package org.jipc.channel.file.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.channels.ClosedChannelException;

import org.jipc.TestUtil;
import org.jipc.channel.file.chunk.ReadableChunkFileChannel;
import org.junit.Test;

/**
 * Tests class {@link ReadableChunkFileChannel}.
 */
public class ReadableChunkFileChannelTest extends AbstractChunkFileChannelTest {

	@Override
	protected ReadableChunkFileChannel createChannel(File directory) {
		return new ReadableChunkFileChannel(directory);
	}
	
	@SuppressWarnings("resource")
	@Test
	public void testGetNextChunk() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		assertNull(channel.getNextChunk());

		File chunk0 = createChunkFile(0);
		File chunk1 = createChunkFile(1);
		File chunk2 = createChunkFile(2);

		assertEquals(chunk0, channel.getNextChunk());
		assertEquals(chunk1, channel.getNextChunk());
		assertEquals(chunk2, channel.getNextChunk());
	}

	@SuppressWarnings("resource")
	@Test
	public void testGetNextChunkMissingChunks() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		assertNull(channel.getNextChunk());

		File chunk2 = createChunkFile(2);
		File chunk5 = createChunkFile(5);
		File chunk8 = createChunkFile(8);

		assertEquals(chunk2, channel.getNextChunk());
		assertEquals(chunk5, channel.getNextChunk());
		assertEquals(chunk8, channel.getNextChunk());
	}

	@SuppressWarnings("resource")
	@Test
	public void testRead() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		assertEquals(0, channel.read(buffer));

		createChunkFile(0, "herbert");
		createChunkFile(1, "karl");
		createChunkFile(2, "fritz");

		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);

		buffer.clear();
		assertEquals(5, channel.read(buffer));
		TestUtil.assertEquals("fritz", buffer);
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadDeletesReadChunks() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		assertEquals(0, channel.read(buffer));

		File chunk0 = createChunkFile(0, "herbert");
		createChunkFile(1, "karl");
		File chunk2 = createChunkFile(2, "fritz");

		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);

		buffer.clear();
		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);

		assertFalse(chunk0.exists());
		// assertFalse(chunk1.exists()); // will be deleted on next read
		assertTrue(chunk2.exists());
	}

	@SuppressWarnings("resource")
	@Test
	public void testEndOfStream() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		assertEquals(0, channel.read(buffer));

		createChunkFile(0, "herbert");
		createChunkFile(1, "karl");

		assertEquals(7, channel.read(buffer));
		TestUtil.assertEquals("herbert", buffer);
		buffer.clear();
		assertEquals(4, channel.read(buffer));
		TestUtil.assertEquals("karl", buffer);

		assertEquals(0, channel.read(buffer));
		closedMarker.createNewFile();
		assertEquals(-1, channel.read(buffer));
	}

	@Test(expected = ClosedChannelException.class)
	public void testCheckClosed() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		channel.close();
		channel.read(buffer);
	}

}
