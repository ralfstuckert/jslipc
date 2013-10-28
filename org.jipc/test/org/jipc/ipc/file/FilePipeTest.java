package org.jipc.ipc.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.ipc.AbstractTestProducer;
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
		File source = FilePipe.getSourceFile(directory, JipcRole.Client);
		checkChannelFile(source, FilePipe.SERVER_TO_CLIENT_NAME);

		source = FilePipe.getSourceFile(directory, JipcRole.Server);
		checkChannelFile(source, FilePipe.CLIENT_TO_SERVER_NAME);
	}

	@Test
	public void testGetSinkFile() throws Exception {
		File source = FilePipe.getSinkFile(directory, JipcRole.Server);
		checkChannelFile(source, FilePipe.SERVER_TO_CLIENT_NAME);

		source = FilePipe.getSinkFile(directory, JipcRole.Client);
		checkChannelFile(source, FilePipe.CLIENT_TO_SERVER_NAME);
	}

	protected void checkChannelFile(File file, String expectedName) {
		assertTrue(file.exists());
		assertEquals(directory, file.getParentFile());
		assertEquals(expectedName, file.getName());
	}

	@SuppressWarnings("resource")
	@Test
	public void testFilePipeFileJipcRole() throws Exception {
		new FilePipe(directory, JipcRole.Client);
		new FilePipe(directory, JipcRole.Server);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNullJipcRole() throws Exception {
		new FilePipe(null, JipcRole.Client);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileNotDirectoryJipcRole() throws Exception {
		new FilePipe(source, JipcRole.Client);
	}

	@SuppressWarnings("resource")
	@Test(expected = IllegalArgumentException.class)
	public void testFilePipeFileJipcRoleNull() throws Exception {
		new FilePipe(directory, (JipcRole) null);
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

		pipe = new FilePipe(directory, JipcRole.Client);
		assertNotNull(pipe.source());
	}

	@SuppressWarnings("resource")
	@Test
	public void testSink() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe = new FilePipe(directory, JipcRole.Client);
		assertNotNull(pipe.sink());
	}

	@SuppressWarnings("resource")
	@Test
	public void testClose() throws Exception {
		FilePipe pipe = new FilePipe(source, sink);
		assertNotNull(pipe.sink());

		pipe = new FilePipe(directory, JipcRole.Client);
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
		assertNotNull(pipe.sink());

		pipe.cleanUpOnClose();
		assertTrue(pipe.source().isOpen());
		assertTrue(pipe.sink().isOpen());

		createClosedMarker(source);
		createClosedMarker(sink);
		pipe.close();
		assertFalse(source.exists());
		assertFalse(sink.exists());
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
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						FilePipeTestProducer.class.getName(),
						consumerToProducer.getAbsolutePath(),
						producerToConsumer.getAbsolutePath() });

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JipcPipe pipe = new FilePipe(producerToConsumer, consumerToProducer);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

	@Test(timeout = 20000)
	public void testIpcWithDirectory() throws Exception {
		Process producer = Runtime.getRuntime().exec(
				new String[] { System.getProperty("java.home") + "/bin/java",
						"-cp", System.getProperty("java.class.path"),
						FilePipeTestProducer.class.getName(),
						directory.getAbsolutePath(), "-" + JipcRole.Server });

		// int date = 0;
		// while ((date = producer.getErrorStream().read()) != -1) {
		// System.err.write(date);
		// }

		Thread.sleep(1000);
		FilePipeTestConsumer consumer = new FilePipeTestConsumer();
		JipcPipe pipe = new FilePipe(directory, JipcRole.Client);

		String reply = consumer.consume(pipe);
		String expectedReply = consumer.createReply(AbstractTestProducer.HELLO);
		assertEquals(expectedReply, reply);

		producer.waitFor();
	}

}
