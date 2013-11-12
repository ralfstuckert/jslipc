package sun.net.www.protocol.jipc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.jipc.JipcPipe;
import org.jipc.TestUtil;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.ReadableJipcByteChannel;
import org.jipc.channel.WritableJipcByteChannel;
import org.jipc.ipc.pipe.JipcConnection;
import org.jipc.ipc.pipe.JipcPipeServer;
import org.jipc.ipc.pipe.JipcPipeURLConnection;
import org.jipc.ipc.pipe.JipcRequest;
import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.ipc.pipe.file.ChunkFilePipe;
import org.jipc.ipc.pipe.file.FilePipe;
import org.jipc.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HandlerTest {

	private File connectDir;
	private File pipeDir;

	@Before
	public void setUp() throws Exception {
		connectDir = TestUtil.createDirectory();
		pipeDir = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(connectDir, true);
		FileUtil.delete(pipeDir, true);
	}

	@Test(expected = IOException.class)
	public void testOpenConnectionWithUnknownServerDir() throws Exception {
		URL url = new URL("jipc:///c:/server/connect");
		url.openConnection();
	}

	@Test(timeout = 60000)
	public void testOpenConnection() throws Exception {
		checkOpenConnection("/" + connectDir.getAbsolutePath());
	}

	@Test(timeout = 60000)
	public void testOpenConnectionWithRelativeServerDir() throws Exception {
		File dir = FileUtil.createDirectory(new File("."));
		checkOpenConnection(dir.getPath());
	}

	public void checkOpenConnection(final String connectDirPath) throws IOException,
			InterruptedException {
		final AtomicReference<JipcConnection> connectRef = new AtomicReference<JipcConnection>();
		Thread thread = new Thread() {
			public void run() {
				JipcPipeServer server;
				try {
					server = new JipcPipeServer(new File(connectDirPath), pipeDir);
					connectRef.set(server.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();

		URL url = new URL("jipc://"+ connectDirPath);
		URLConnection urlConnection = url.openConnection(); 
		assertNotNull(urlConnection);
		assertEquals(JipcPipeURLConnection.class, urlConnection.getClass());

		thread.join();
		assertFalse(thread.isAlive());

		JipcPipe serverSidePipe = connectRef.get().getPipe();
		checkConnection(urlConnection.getOutputStream(),
				serverSidePipe.source());
		checkConnection(serverSidePipe.sink(), urlConnection.getInputStream());
	}

	@Test
	public void testCreateRequest() throws Exception {
		Handler handler = new Handler();
		URL url = new URL("jipc:///c:/server/connect");
		JipcRequest request = handler.createRequest(url);
		assertNotNull(request);
		assertEquals(JipcCommand.CONNECT, request.getCommand());
		assertEquals(new File("c:/server/connect"),
				request.getFileParameter(JipcRequest.PARAM_DIRECTORY));
		assertEquals(1, request.getParameters().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateRequestWithParameter() throws Exception {
		Handler handler = new Handler();
		URL url = new URL(
				"jipc:///c:/server/connect?accept-types=ChunkFilePipe,FilePipe&special-guest=spongebob&happy=&x");
		JipcRequest request = handler.createRequest(url);
		assertNotNull(request);
		assertEquals(JipcCommand.CONNECT, request.getCommand());
		assertEquals(new File("c:/server/connect"),
				request.getFileParameter(JipcRequest.PARAM_DIRECTORY));
		assertEquals(3, request.getParameters().size());

		assertEquals(Arrays.asList(ChunkFilePipe.class, FilePipe.class),
				request.getAcceptTypes());
		assertEquals("spongebob", request.getParameter("special-guest"));
		assertEquals(null, request.getParameter("happy"));
		assertEquals(null, request.getParameter("x"));
		assertEquals(null, request.getParameter("y"));
	}

	private void checkConnection(final OutputStream out,
			final ReadableJipcByteChannel source) throws IOException {
		checkConnection(out, new JipcChannelInputStream(source));
	}

	private void checkConnection(final WritableJipcByteChannel sink,
			final InputStream in) throws IOException {
		checkConnection(new JipcChannelOutputStream(sink), in);
	}

	private void checkConnection(final OutputStream out, final InputStream in)
			throws IOException {
		byte[] written = new byte[] { 17, 68, 89 };
		byte[] read = new byte[3];
		out.write(written);
		out.flush();

		in.read(read);
		assertArrayEquals(written, read);
	}

}
