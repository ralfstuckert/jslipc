package org.jipc.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.TestUtil;
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
 * Tests the {@link JipcPipeServer}.
 */
public class JipcPipeServerTest {

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
	public void testJipcPipeServer() throws Exception {
		new JipcPipeServer(directory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithNull() throws Exception {
		new JipcPipeServer(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithNonExistingDir() throws Exception {
		new JipcPipeServer(new File("herbert"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJipcPipeServerWithFile() throws Exception {
		File file = File.createTempFile("xxx", ".tmp");
		file.deleteOnExit();
		new JipcPipeServer(file);
	}

	@SuppressWarnings("unchecked")
	@Test(timeout = 600000)
	public void testAccept() throws Exception {

		final AtomicReference<JipcConnection> pipeRef = new AtomicReference<JipcConnection>();
		Thread thread = new Thread() {
			public void run() {
				JipcPipeServer server;
				try {
					server = new JipcPipeServer(directory);
					pipeRef.set(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		File connectDir = FileUtil.createDirectory(directory);
		FilePipe connectPipe = new FilePipe(connectDir, JipcRole.Yang);
		connectPipe.cleanUpOnClose();

		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(ChunkFilePipe.class);
		writeRequest(connectPipe, request);

		JipcResponse response = readResponse(connectPipe);
		assertEquals(ChunkFilePipe.class, response.getTypeParameter());
		File pipeDir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
		assertNotNull(pipeDir);
		assertTrue(pipeDir.exists());

		connectPipe.close();

		thread.join();
		JipcConnection connection = pipeRef.get();
		assertNotNull(connection);
		assertEquals(request.getParameters(), connection.getRequestParameters());
		assertNotNull(connection.getPipe());
		assertEquals(ChunkFilePipe.class, connection.getPipe().getClass());
	}

	@Test(timeout = 10000)
	public void testWaitForDirectory() throws Exception {
		File dir = FileUtil.createDirectory(directory);
		final JipcPipeServer server = new JipcPipeServer(directory);
		assertEquals(dir, server.waitForDirectory());

		final AtomicReference<File> fileRef = new AtomicReference<File>();
		Thread thread = new Thread() {
			public void run() {
				try {
					fileRef.set(server.waitForDirectory());
				} catch (IOException e) {
//					e.printStackTrace();
				}
			}
		};
		thread.start();

		thread.join(3000);
		assertTrue(thread.isAlive());

		thread.interrupt();
		thread.join(3000);
		assertFalse(thread.isAlive());
	}

	@SuppressWarnings("unchecked")
	public void testCreatePipe() throws Exception {
		final JipcPipeServer server = new JipcPipeServer(directory);

		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(SharedMemoryPipe.class);
		JipcResponse response = new JipcResponse(JipcCode.PipeCreated, "ok");
		JipcPipe pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(SharedMemoryPipe.class, pipe.getClass());
		assertEquals(SharedMemoryPipe.class, response.getTypeParameter());
		assertEquals(JipcRole.Yang, response.getRoleParameter());
		assertNotNull(response.getFileParameter(JipcResponse.PARAM_FILE));
		assertTrue(response.getFileParameter(JipcResponse.PARAM_FILE).exists());

		request.setAcceptTypes(FilePipe.class, ChunkFilePipe.class, SharedMemoryPipe.class);
		response = new JipcResponse(JipcCode.PipeCreated, "ok");
		pipe = server.createPipe(request, response);
		assertNotNull(pipe);
		assertEquals(ChunkFilePipe.class, pipe.getClass());
		assertEquals(ChunkFilePipe.class, response.getTypeParameter());
		assertEquals(JipcRole.Yang, response.getRoleParameter());
		assertNotNull(response.getFileParameter(JipcResponse.PARAM_DIRECTORY));
		assertTrue(response.getFileParameter(JipcResponse.PARAM_DIRECTORY).exists());
	}

	private JipcResponse readResponse(FilePipe connectPipe) throws IOException {
		InputStream in = new JipcChannelInputStream(connectPipe.source());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();
		JipcResponse response = new JipcResponse(baos.toByteArray());
		return response;
	}

	private void writeRequest(FilePipe connectPipe, JipcRequest request)
			throws IOException {
		OutputStream out = new JipcChannelOutputStream(connectPipe.sink());
		out.write(request.toBytes());
		out.close();
	}

}
