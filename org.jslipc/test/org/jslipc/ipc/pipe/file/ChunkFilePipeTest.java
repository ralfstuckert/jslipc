package org.jslipc.ipc.pipe.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TestUtil;
import org.jslipc.ipc.pipe.AbstractTestProducer;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
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
		File source = ChunkFilePipe.getSourceDir(directory, JslipcRole.Yang);
		checkChannelDir(source, ChunkFilePipe.YIN_TO_YANG_NAME);

		source = ChunkFilePipe.getSourceDir(directory, JslipcRole.Yin);
		checkChannelDir(source, ChunkFilePipe.YANG_TO_YIN_NAME);
	}

	@Test
	public void testGetSinkFile() throws Exception {
		File source = ChunkFilePipe.getSinkDir(directory, JslipcRole.Yin);
		checkChannelDir(source, ChunkFilePipe.YIN_TO_YANG_NAME);

		source = ChunkFilePipe.getSinkDir(directory, JslipcRole.Yang);
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
	public void testChunkFilePipeFileJslipcRole() throws Exception {
		new ChunkFilePipe(directory, JslipcRole.Yang);
		new ChunkFilePipe(directory, JslipcRole.Yin);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileNullJslipcRole() throws Exception {
		new ChunkFilePipe(null, JslipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileNotDirectoryJslipcRole() throws Exception {
		File tmpFile = File.createTempFile("xxx", ".tmp");
		tmpFile.deleteOnExit();
		new ChunkFilePipe(tmpFile, JslipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testChunkFilePipeFileJslipcRoleNull() throws Exception {
		new ChunkFilePipe(directory, (JslipcRole) null);
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

		pipe = new ChunkFilePipe(directory, JslipcRole.Yang);
		assertNotNull(pipe.source());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSink() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.sink());

		pipe = new ChunkFilePipe(directory, JslipcRole.Yang);
		assertNotNull(pipe.sink());
	}

	@SuppressWarnings("resource")
	@Test
	public void testClose() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);
		assertNotNull(pipe.sink());

		pipe = new ChunkFilePipe(directory, JslipcRole.Yang);
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
	public void testIssue15() throws Exception {
		ChunkFilePipe pipe = new ChunkFilePipe(sourceDir, sinkDir);

		pipe.cleanUpOnClose();
		// neither call source() nor sink()
//		assertTrue(pipe.source().isOpen());
//		assertTrue(pipe.sink().isOpen());

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
		ChunkFilePipe pipe = new ChunkFilePipe(directory, JslipcRole.Yang);

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
				new String[] { TestUtil.getJvm(),
						"-cp", TestUtil.getTestClassPath(),
						ChunkFilePipeTestProducer.class.getName(),
						consumerToProducer.getAbsolutePath(),
						producerToConsumer.getAbsolutePath() });

//		 int date = 0;
//		 while ((date = producer.getInputStream().read()) != -1) {
//		 System.err.write(date);
//		 }
		
		Thread.sleep(1000);
		ChunkFilePipeTestConsumer consumer = new ChunkFilePipeTestConsumer();
		JslipcPipe pipe = new ChunkFilePipe(producerToConsumer, consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

	@Test(timeout = 20000)
	public void testIpcWithDirectoryAndRole() throws Exception {
		Process producer = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(),
						"-cp", TestUtil.getTestClassPath(),
						ChunkFilePipeTestProducer.class.getName(),
						directory.getAbsolutePath(), "-" + JslipcRole.Yin });
		
//		 int date = 0;
//		 while ((date = producer.getErrorStream().read()) != -1) {
//		 System.err.write(date);
//		 }

		Thread.sleep(1000);
		ChunkFilePipeTestConsumer consumer = new ChunkFilePipeTestConsumer();
		JslipcPipe pipe = consumer.createPipe(directory, JslipcRole.Yang);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}


	
}
