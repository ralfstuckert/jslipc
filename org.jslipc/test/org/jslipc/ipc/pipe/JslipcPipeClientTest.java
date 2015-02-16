package org.jslipc.ipc.pipe;

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

import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TestUtil;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.ipc.pipe.JslipcResponse.JslipcCode;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jslipc.util.FileUtil;
import org.jslipc.util.UrlUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JslipcPipeClient}.
 */
public class JslipcPipeClientTest {

	private File directory;

	@Before
	public void setUp() throws Exception {
		directory = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(directory, true);
	}

	public void testJslipcPipeClient() throws Exception {
		new JslipcPipeClient(directory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJslipcPipeClientWithNullFile() throws Exception {
		new JslipcPipeClient((File)null);
	}

	@Test(expected = IOException.class)
	public void testJslipcPipeClientWithNonExistingDir() throws Exception {
		new JslipcPipeClient(new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJslipcPipeClientWithFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JslipcPipeClient(file);
	}

	@Test
	public void testGetServerDirectory() throws Exception {
		JslipcPipeClient client = new JslipcPipeClient(directory);
		assertEquals(directory, client.getServerConnectDirectory());
	}

	@Test(timeout = 600000)
	public void testConnect() throws Exception {

		final AtomicReference<JslipcPipe> pipeRef = new AtomicReference<JslipcPipe>();
		Thread thread = new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				JslipcPipeClient client;
				try {
					client = new JslipcPipeClient(directory);
					pipeRef.set(client.connect(ChunkFilePipe.class));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = waitForDirectory();
		FilePipe connectPipe = new FilePipe(connectDir, JslipcRole.Yin);
		connectPipe.cleanUpOnClose();
		JslipcRequest request = readRequest(connectPipe);
		assertEquals(JslipcCommand.CONNECT, request.getCommand());
		assertEquals("ChunkFilePipe",
				request.getParameter(JslipcRequest.PARAM_ACCEPT_TYPES));

		File pipeDir = FileUtil.createDirectory(directory);
		JslipcResponse response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		response.setFileParameter(JslipcResponse.PARAM_DIRECTORY, pipeDir);
		response.setParameter(JslipcResponse.PARAM_ROLE, JslipcRole.Yang.toString());
		response.setTypeParameter(ChunkFilePipe.class);
		writeResponse(connectPipe, response);
		connectPipe.close();

		thread.join();
		assertNotNull(pipeRef.get());
		assertEquals(ChunkFilePipe.class, pipeRef.get().getClass());
	}

	private void writeResponse(FilePipe connectPipe, JslipcResponse response)
			throws IOException {
		OutputStream out = new JslipcChannelOutputStream(connectPipe.sink());
		out.write(response.toBytes());
		out.close();
	}

	private JslipcRequest readRequest(FilePipe connectPipe) throws IOException {
		InputStream in = new JslipcChannelInputStream(connectPipe.source());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		JslipcRequest request = new JslipcRequest(baos.toByteArray());
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
	public void testSendRequest() throws Exception {
		JslipcPipeClient client = new JslipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JSLIPC/1.0", requestLines.get(0));
	}

	@Test
	public void testRequestPipeWithAcceptedTypes() throws Exception {
		JslipcPipeClient client = new JslipcPipeClient(directory);
		@SuppressWarnings("unchecked")
		List<String> requestLines = sendRequest(client, FilePipe.class,
				ChunkFilePipe.class);

		assertTrue(requestLines.toString(), requestLines.size() > 0);
		assertEquals("CONNECT JSLIPC/1.0", requestLines.get(0));
		assertThat(
				requestLines,
				hasItem(AbstractJslipcMessage.PARAM_ACCEPT_TYPES+": "
						+ UrlUtil.urlEncode("FilePipe,ChunkFilePipe")));
	}

	protected List<String> sendRequest(final JslipcPipeClient client,
			Class<? extends JslipcPipe>... acceptedTypes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JslipcRequest request = new JslipcRequest(JslipcCommand.CONNECT);
		request.setAcceptTypes(acceptedTypes);
		client.sendRequest(baos, request);
		String requestString = new String(baos.toByteArray());
		return Arrays.asList(requestString.split("\\n"));
	}

	@Test
	public void testReadResponse() throws Exception {
		JslipcPipeClient client = new JslipcPipeClient(directory);

		checkReadResponseFilePipe(client);
		checkReadResponseChunkFilePipe(client);
		checkReadResponseSharedMemoryPipe(client);
	}

	private void checkReadResponseFilePipe(JslipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JslipcResponse response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		response.setTypeParameter(FilePipe.class);
		response.setParameter(JslipcResponse.PARAM_ROLE, JslipcRole.Yang.toString());
		response.setFileParameter(JslipcResponse.PARAM_DIRECTORY, dir);

		JslipcPipe pipe = readResponse(client, response);
		assertNotNull(pipe);
		assertEquals(FilePipe.class, pipe.getClass());
		File sink = FilePipe.getSinkFile(dir, JslipcRole.Yang);
		assertTrue(sink.exists());
		File source = FilePipe.getSourceFile(dir, JslipcRole.Yang);
		assertTrue(source.exists());
	}

	private void checkReadResponseChunkFilePipe(JslipcPipeClient client)
			throws UnsupportedEncodingException, IOException {

		File dir = TestUtil.createDirectory();
		JslipcResponse response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		response.setTypeParameter(ChunkFilePipe.class);
		response.setParameter(JslipcResponse.PARAM_ROLE, JslipcRole.Yang.toString());
		response.setFileParameter(JslipcResponse.PARAM_DIRECTORY, dir);

		JslipcPipe pipe = readResponse(client, response);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		File sink = ChunkFilePipe.getSinkDir(dir, JslipcRole.Yang);
		assertTrue(sink.exists());
		assertTrue(sink.isDirectory());
		File source = ChunkFilePipe.getSourceDir(dir, JslipcRole.Yang);
		assertTrue(source.exists());
		assertTrue(source.isDirectory());
	}

	private void checkReadResponseSharedMemoryPipe(JslipcPipeClient client)
			throws Exception {

		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		JslipcResponse response = new JslipcResponse(JslipcCode.PipeCreated, "ok");
		response.setTypeParameter(SharedMemoryPipe.class);
		response.setParameter(JslipcResponse.PARAM_ROLE, JslipcRole.Yang.toString());
		response.setFileParameter(JslipcResponse.PARAM_FILE, file);
		response.setIntParameter(JslipcResponse.PARAM_SIZE, 8292);

		JslipcPipe pipe = readResponse(client, response);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
	}

	private JslipcPipe readResponse(JslipcPipeClient client,
			JslipcResponse response) throws IOException {
		InputStream in = new ByteArrayInputStream(response.toBytes());

		JslipcPipe pipe = client.readResponse(in);
		return pipe;
	}

}
