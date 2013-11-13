package org.jipc.ipc.pipe.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.ipc.pipe.AbstractTestProducer;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for class {@link ChunkFilePipe}.
 */
public class ChunkFilePipeTest {

	private File sourceDir;
	private File sinkDir;
	private File directory;

	@Before
	public void setUp() throws Exception {
		sourceDir = TestUtil.createDirectory();
		sourceDir.deleteOnExit();
		sinkDir = TestUtil.createDirectory();
		sinkDir.deleteOnExit();
		
		directory = TestUtil.createDirectory();
		directory.deleteOnExit();
	}

	@Test
	public void testGetSourceFile() throws Exception {
		File source = ChunkFilePipe.getSourceDir(directory, JipcRole.Yang);
		checkChannelDir(source, ChunkFilePipe.YIN_TO_YANG_NAME);

		source = ChunkFilePipe.getSourceDir(directory, JipcRole.Yin);
		checkChannelDir(source, ChunkFilePipe.YANG_TO_YIN_NAME);
	}

	@Test
	public void testGetSinkFile() throws Exception {
		File source = ChunkFilePipe.getSinkDir(directory, JipcRole.Yin);
		checkChannelDir(source, ChunkFilePipe.YIN_TO_YANG_NAME);

		source = ChunkFilePipe.getSinkDir(directory, JipcRole.Yang);
		checkChannelDir(source, ChunkFilePipe.YANG_TO_YIN_NAME);
	}

	protected void checkChannelDir(File file, String expectedName) {
		assertTrue(file.exists());
		assertTrue(file.isDirectory());
		assertEquals(directory, file.getParentFile());
		assertEquals(expectedName, file.getName());
	}

	@SuppressWarnings("resource")
	@Test
	public void testChunkFilePipeFileJipcRole() throws Exception {
		new ChunkFilePipe(directory, JipcRole.Yang);
		new ChunkFilePipe(directory, JipcRole.Yin);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileNullJipcRole() throws Exception {
		new ChunkFilePipe(null, JipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileNotDirectoryJipcRole() throws Exception {
		File tmpFile = File.createTempFile("xxx", ".tmp");
		tmpFile.deleteOnExit();
		new ChunkFilePipe(tmpFile, JipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileJipcRoleNull() throws Exception {
		new ChunkFilePipe(directory, (JipcRole) null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testChunkFilePipeFileFile() throws Exception {
		new ChunkFilePipe(sourceDir, sinkDir);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileNullFile() throws Exception {
		new ChunkFilePipe(sourceDir, (File) null);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeNullFileFile() throws Exception {
		new ChunkFilePipe((File) null, sinkDir);
	}

	@SuppressWarnings("resource")
	@Test
	public void testSource() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.source());

		pipe = new ChunkFilePipe(directory, JipcRole.Yang);
		assertNotNull(pipe.source());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSink() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.sink());

		pipe = new ChunkFilePipe(directory, JipcRole.Yang);
		assertNotNull(pipe.sink());
	}

	@SuppressWarnings("resource")
	@Test
	public void testClose() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.sink());

		pipe = new ChunkFilePipe(directory, JipcRole.Yang);
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		pipe.close();
		assertFalse(pipe.source().isOpen());
		assertFalse(pipe.sink().isOpen());
	}

	@Test
	public void testCleanUpOnCloseStillUsedByPeer() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.sink());

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		pipe.close();
		assertTrue(sourceDir.exists());
		assertTrue(sinkDir.exists());
	}

	@Test
	public void testCleanUpOnClose() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		createClosedMarker(sourceDir);
		createClosedMarker(sinkDir);

		assertTrue(sourceDir.exists());
		assertTrue(sinkDir.exists());
		pipe.close();
		String[] files = sourceDir.list();
		assertTrue("not deleted: " + toString(files),
				files == null || files.length == 0);

		assertFalse(sourceDir.exists());
		assertFalse(sinkDir.exists());
	}

	@Test
	public void testCleanUpOnCloseWithDirAndRole() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(directory, JipcRole.Yang);

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		createClosedMarker(new File(directory, ChunkFilePipe.YANG_TO_YIN_NAME));
		createClosedMarker(new File(directory, ChunkFilePipe.YIN_TO_YANG_NAME));

		assertTrue(directory.exists());
		pipe.close();
		assertFalse(directory.exists());
	}

	protected String toString(Object[] array) {
		if (array == null) {
			return "null";
		}
		return Arrays.asList(array).toString();
	}

	private void createClosedMarker(final File directory) throws IOException {
		assertTrue(new File(directory, ".closed").createNewFile());
	}

	@Test(timeout = 20000)
	public void testIpcWithChannels() throws Exception {
		File consumerToProducer = TestUtil.createDirectory();
		consumerToProducer.deleteOnExit();
		File producerToConsumer = TestUtil.createDirectory();
		producerToConsumer.deleteOnExit();

		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						ChunkFilePipeTestProducer.class.getName(),
						consumerToProducer.getAbsolutePath(),
						producerToConsumer.getAbsolutePath() });

//		 int date = 0;
//		 while ((date = producer.getErrorStream().read()) != -1) {
//		 System.err.write(date);
//		 }
		
		Thread.sleep(1000);
		ChunkFilePipeTestConsumer consumer = new ChunkFilePipeTestConsumer();
		JipcPipe pipe = new ChunkFilePipe(producerToConsumer, consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

	@Test(timeout = 20000)
	public void testIpcWithDirectoryAndRole() throws Exception {
		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						ChunkFilePipeTestProducer.class.getName(),
						directory.getAbsolutePath(), "-" + JipcRole.Yin });
		
//		 int date = 0;
//		 while ((date = producer.getErrorStream().read()) != -1) {
//		 System.err.write(date);
//		 }

		Thread.sleep(1000);
		ChunkFilePipeTestConsumer consumer = new ChunkFilePipeTestConsumer();
		JipcPipe pipe = consumer.createPipe(directory, JipcRole.Yang);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}


}
