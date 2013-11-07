package org.jipc.ipc;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
import org.jipc.UrlUtil;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.file.FileUtil;
import org.jipc.ipc.JipcRequest.JipcCommand;
import org.jipc.ipc.JipcResponse.JipcCode;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JipcPipeClient}.
 */
public class JipcPipeClientTest {

	private File directory;

	@Before
	public void setUp() throws Exception {
		directory = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(directory, true);
	}

	@Test
	public void testJipcPipeClient() throws Exception {
		new JipcPipeClient(directory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithNull() throws Exception {
		new JipcPipeClient(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithNonExistingDir() throws Exception {
		new JipcPipeClient(new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeClientWithFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JipcPipeClient(file);
	}

	@Test(timeout = 600000)
	public void testConnect() throws Exception {

		final AtomicReference<JipcPipe> pipeRef = new AtomicReference<JipcPipe>();
		Thread thread = new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				JipcPipeClient client;
				try {
					client = new JipcPipeClient(directory);
					pipeRef.set(client.connect(ChunkFilePipe.class));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = waitForDirectory();
		FilePipe connectPipe = new FilePipe(connectDir, JipcRole.Yin);
		connectPipe.cleanUpOnClose();
		JipcRequest request = readRequest(connectPipe);
		assertEquals(JipcCommand.CONNECT, request.getCommand());
		assertEquals("ChunkFilePipe",
				request.getParameter(JipcRequest.PARAM_ACCEPT_TYPES));

		File pipeDir = FileUtil.createDirectory(directory);
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		response.setParameter(JipcResponse.PARAM_DIRECTORY,
				pipeDir.getAbsolutePath());
		response.setParameter(JipcResponse.PARAM_ROLE, JipcRole.Yang.toString());
		response.setParameter(JipcResponse.PARAM_TYPE,
				ChunkFilePipe.class.getSimpleName());
		writeResponse(connectPipe, response);
		connectPipe.close();

		thread.join();
		assertNotNull(pipeRef.get());
		assertEquals(ChunkFilePipe.class, pipeRef.get().getClass());
	}

	private void writeResponse(FilePipe connectPipe, JipcResponse response)
			throws IOException {
		OutputStream out = new JipcChannelOutputStream(connectPipe.sink());
		out.write(response.toBytes());
		out.close();
	}

	private JipcRequest readRequest(FilePipe connectPipe) throws IOException {
		InputStream in = new JipcChannelInputStream(connectPipe.source());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		JipcRequest request = new JipcRequest(baos.toByteArray());
		return request;
	}

	private File waitForDirectory() throws InterruptedException {
		File dir = null;
		while (dir == null) {
			File[] files = directory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					dir = file;
				}
			}
			Thread.sleep(200);
		}
		return dir;
	}

	@Test
	public void testRequestPipe() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JIPC/1.0", requestLines.get(0));
	}

	@Test
	public void testRequestPipeWithAcceptedTypes() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client, FilePipe.class,
				ChunkFilePipe.class);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JIPC/1.0", requestLines.get(0));
		assertThat(
				requestLines,
				hasItem("ACCEPT-TYPES: "
						+ UrlUtil.urlEncode("FilePipe,ChunkFilePipe")));
	}

	protected List<String> sendRequest(final JipcPipeClient client,
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		client.requestPipe(baos, acceptedTypes);
		String request = new String(baos.toByteArray());
		return Arrays.asList(request.split("\\n"));
	}

	@Test
	public void testWaitForResponse() throws Exception {
		JipcPipeClient client = new JipcPipeClient(directory);

		checkWaitForFilePipe(client);
		checkWaitForChunkFilePipe(client);
		checkWaitForSharedMemoryPipe(client);
	}

	private void checkWaitForFilePipe(JipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		response.setParameter(JipcResponse.PARAM_TYPE,
				FilePipe.class.getSimpleName());
		response.setParameter(JipcResponse.PARAM_ROLE, JipcRole.Yang.toString());
		response.setParameter(JipcResponse.PARAM_DIRECTORY,
				dir.getAbsolutePath());

		JipcPipe pipe = waitForResponse(client, response);
		assertNotNull(pipe);
		assertEquals(FilePipe.class, pipe.getClass());
		File sink = FilePipe.getSinkFile(dir, JipcRole.Yang);
		assertTrue(sink.exists());
		File source = FilePipe.getSourceFile(dir, JipcRole.Yang);
		assertTrue(source.exists());
	}

	private void checkWaitForChunkFilePipe(JipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		response.setParameter(JipcResponse.PARAM_TYPE,
				ChunkFilePipe.class.getSimpleName());
		response.setParameter(JipcResponse.PARAM_ROLE, JipcRole.Yang.toString());
		response.setParameter(JipcResponse.PARAM_DIRECTORY,
				dir.getAbsolutePath());

		JipcPipe pipe = waitForResponse(client, response);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		File sink = ChunkFilePipe.getSinkDir(dir, JipcRole.Yang);
		assertTrue(sink.exists());
		assertTrue(sink.isDirectory());
		File source = ChunkFilePipe.getSourceDir(dir, JipcRole.Yang);
		assertTrue(source.exists());
		assertTrue(source.isDirectory());
	}

	private void checkWaitForSharedMemoryPipe(JipcPipeClient client)
			throws Exception {

		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		response.setParameter(JipcResponse.PARAM_TYPE,
				SharedMemoryPipe.class.getSimpleName());
		response.setParameter(JipcResponse.PARAM_ROLE, JipcRole.Yang.toString());
		response.setParameter(JipcResponse.PARAM_FILE, file.getAbsolutePath());
		response.setParameter(JipcResponse.PARAM_SIZE, Integer.toString(8292));

		JipcPipe pipe = waitForResponse(client, response);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
	}

	private JipcPipe waitForResponse(JipcPipeClient client,
			JipcResponse response) throws IOException {
		InputStream in = new ByteArrayInputStream(response.toBytes());

		JipcPipe pipe = client.waitForResponse(in);
		return pipe;
	}

}
