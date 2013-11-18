package org.jslipc.channel.file.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.jslipc.TestUtil;
import org.jslipc.channel.file.chunk.ReadableChunkFileChannel;
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

		assertEquals(16, channel.read(buffer));
		TestUtil.assertEquals("herbertkarlfritz", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadChunkPartially() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		ByteBuffer smallBuffer = TestUtil.createByteBuffer(5);

		assertEquals(0, channel.read(smallBuffer));

		createChunkFile(0, "herbert");
		createChunkFile(1, "karl");
		createChunkFile(2, "fritz");

		assertEquals(5, channel.read(smallBuffer));
		TestUtil.assertEquals("herbe", smallBuffer);

		smallBuffer.clear();
		assertEquals(5, channel.read(smallBuffer));
		TestUtil.assertEquals("rtkar", smallBuffer);

		smallBuffer.clear();
		assertEquals(5, channel.read(smallBuffer));
		TestUtil.assertEquals("lfrit", smallBuffer);

		smallBuffer.clear();
		assertEquals(1, channel.read(smallBuffer));
		TestUtil.assertEquals("z", smallBuffer);
	}

	@SuppressWarnings("resource")
	@Test
	public void testReadDeletesReadChunks() throws Exception {
		ReadableChunkFileChannel channel = new ReadableChunkFileChannel(
				directory);
		ByteBuffer smallBuffer = TestUtil.createByteBuffer(15);
		assertEquals(0, channel.read(smallBuffer));

		File chunk0 = createChunkFile(0, "herbert");
		File chunk1 = createChunkFile(1, "karl");
		File chunk2 = createChunkFile(2, "fritz");

		assertEquals(15, channel.read(smallBuffer));
		TestUtil.assertEquals("herbertkarlfrit", smallBuffer);

		assertFalse(chunk0.exists());
		assertFalse(chunk1.exists()); 
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

		assertEquals(11, channel.read(buffer));
		TestUtil.assertEquals("herbertkarl", buffer);

		buffer.clear();
		assertEquals(0, channel.read(buffer));
		
		// closed by peer
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
