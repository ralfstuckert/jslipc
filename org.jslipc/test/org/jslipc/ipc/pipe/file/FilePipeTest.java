package org.jslipc.ipc.pipe.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TestUtil;
import org.jslipc.ipc.pipe.AbstractTestProducer;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.junit.Before;
import org.junit.Test;

public class FilePipeTest {

	private File source;
	private File sink;
	private File directory;

	@Before
	public void setUp() throws Exception {
		source = File.createTempFile("inn", ".pipe");
		source.deleteOnExit();
		sink = File.createTempFile("out", ".pipe");
		sink.deleteOnExit();

		directory = TestUtil.createDirectory();
		directory.deleteOnExit();
	}

	@Test
	public void testGetSourceFile() throws Exception {
		File source = FilePipe.getSourceFile(directory, JslipcRole.Yang);
		checkChannelFile(source, FilePipe.YIN_TO_YANG_NAME);

		source = FilePipe.getSourceFile(directory, JslipcRole.Yin);
		checkChannelFile(source, FilePipe.YANG_TO_YIN_NAME);
	}

	@Test
	public void testGetSinkFile() throws Exception {
		File source = FilePipe.getSinkFile(directory, JslipcRole.Yin);
		checkChannelFile(source, FilePipe.YIN_TO_YANG_NAME);

		source = FilePipe.getSinkFile(directory, JslipcRole.Yang);
		checkChannelFile(source, FilePipe.YANG_TO_YIN_NAME);
	}

	protected void checkChannelFile(File file, String expectedName) {
		assertTrue(file.exists());
		assertEquals(directory, file.getParentFile());
		assertEquals(expectedName, file.getName());
	}

	@SuppressWarnings("resource")
	@Test
	public void testFilePipeFileJslipcRole() throws Exception {
		new FilePipe(directory, JslipcRole.Yang);
		new FilePipe(directory, JslipcRole.Yin);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNullJslipcRole() throws Exception {
		new FilePipe(null, JslipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNotDirectoryJslipcRole() throws Exception {
		new FilePipe(source, JslipcRole.Yang);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileJslipcRoleNull() throws Exception {
		new FilePipe(directory, (JslipcRole) null);
	}

	@SuppressWarnings("resource")
	@Test
	public void testFilePipeFileFile() throws Exception {
		new FilePipe(source, sink);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNullFile() throws Exception {
		new FilePipe(source, (File) null);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeNullFileFile() throws Exception {
		new FilePipe((File) null, sink);
	}

	@SuppressWarnings("resource")
	@Test
	public void testSource() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.source());

		pipe = new FilePipe(directory, JslipcRole.Yang);
		assertNotNull(pipe.source());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSink() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe = new FilePipe(directory, JslipcRole.Yang);
		assertNotNull(pipe.sink());
	}

	@SuppressWarnings("resource")
	@Test
	public void testClose() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe = new FilePipe(directory, JslipcRole.Yang);
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		pipe.close();
		assertFalse(pipe.source().isOpen());
		assertFalse(pipe.sink().isOpen());
	}

	@Test
	public void testCleanUpOnCloseStillUsedByPeer() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		pipe.close();
		assertTrue(source.exists());
		assertTrue(sink.exists());
	}

	@Test
	public void testCleanUpOnClose() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		createClosedMarker(source);
		createClosedMarker(sink);

		assertTrue(source.exists());
		assertTrue(sink.exists());
		pipe.close();
		assertFalse(source.exists());
		assertFalse(sink.exists());
	}

	@Test
	public void testIssue15() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);

		pipe.cleanUpOnClose();
		// neither call source() nor sink()
		// assertTrue(pipe.source().isOpen());
		// assertTrue(pipe.sink().isOpen());

		createClosedMarker(source);
		createClosedMarker(sink);

		assertTrue(source.exists());
		assertTrue(sink.exists());
		pipe.close();
		// assertFalse(source.exists());
		assertFalse(sink.exists());
	}

	@Test
	public void testCleanUpOnCloseWithDirAndRole() throws Exception {
		FilePipe pipe = new FilePipe(directory, JslipcRole.Yang);

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		createClosedMarker(new File(directory, FilePipe.YANG_TO_YIN_NAME));
		createClosedMarker(new File(directory, FilePipe.YIN_TO_YANG_NAME));

		assertTrue(directory.exists());
		pipe.close();
		assertFalse(directory.exists());
	}

	private void createClosedMarker(final File file) throws IOException {
		assertTrue(new File(file.getAbsolutePath() + ".closed").createNewFile());
	}

	@Test(timeout = 20000)
	public void testIpcWithFileChannels() throws Exception {
		File consumerToProducer = File.createTempFile("consumerToProducer",
				".pipe");
		consumerToProducer.deleteOnExit();
		File producerToConsumer = File.createTempFile("producerToConsumer",
				".pipe");
		producerToConsumer.deleteOnExit();

		Process producer = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(),
						"-cp", TestUtil.getTestClassPath(),
						FilePipeTestProducer.class.getName(),
						consumerToProducer.getAbsolutePath(),
						producerToConsumer.getAbsolutePath() });

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JslipcPipe pipe = new FilePipe(producerToConsumer, consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

	@Test(timeout = 20000)
	public void testIpcWithDirectory() throws Exception {
		Process producer = Runtime.getRuntime().exec(
				new String[] { TestUtil.getJvm(),
						"-cp", TestUtil.getTestClassPath(),
						FilePipeTestProducer.class.getName(),
						directory.getAbsolutePath(), "-" + JslipcRole.Yin });

		// int date = 0;
		// while ((date = producer.getErrorStream().read()) != -1) {
		// System.err.write(date);
		// }

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JslipcPipe pipe = new FilePipe(directory, JslipcRole.Yang);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

}
