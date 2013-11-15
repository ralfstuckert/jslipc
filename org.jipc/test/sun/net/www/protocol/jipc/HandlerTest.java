package sun.net.www.protocol.jipc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
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
import org.jipc.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HandlerTest {

	private File connectDir;
	private File pipeDir;
	private File dir;

	@Before
	public void setUp() throws Exception {
		connectDir = TestUtil.createDirectory();
		pipeDir = TestUtil.createDirectory();
	}

	@After
	public void tearDown() throws Exception {
		FileUtil.delete(connectDir, true);
		FileUtil.delete(pipeDir, true);
		if (dir != null) {
			FileUtil.delete(dir, true);
		}
	}

	@Test(expected = IOException.class)
	public void testOpenConnectionWithUnknownServerDir() throws Exception {
		URL url = new URL("jipc:///c:/server/connect");
		url.openConnection();
	}

	@Test(timeout = 60000)
	public void testOpenConnectionWithAbsoluteServerDir() throws Exception {
		checkOpenConnection("/" + connectDir.getAbsolutePath());
	}

	@Test(timeout = 60000)
	public void testOpenConnectionWithRelativeServerDir() throws Exception {
		dir = FileUtil.createDirectory(new File("."));
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

		// not yet connected, server still waiting
		thread.join(200);
		assertTrue(thread.isAlive());

		// now connect
		urlConnection.connect();
		thread.join();
		assertFalse(thread.isAlive());

		// server responded, check pipe
		JipcPipe serverSidePipe = connectRef.get().getPipe();
		checkConnection(urlConnection.getOutputStream(),
				serverSidePipe.source());
		checkConnection(serverSidePipe.sink(), urlConnection.getInputStream());
		
		urlConnection.getInputStream().close();
		urlConnection.getOutputStream().close();
		serverSidePipe.source().close();
		serverSidePipe.sink().close();
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
