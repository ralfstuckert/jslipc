package org.jslipc.ipc.pipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.JslipcRole;
import org.jslipc.TimeoutAware;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.ipc.pipe.JslipcResponse.JslipcCode;
import org.jslipc.ipc.pipe.file.ChunkFilePipe;
import org.jslipc.ipc.pipe.file.FilePipe;
import org.jslipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jslipc.util.FileUtil;
import org.jslipc.util.HostDir;
import org.jslipc.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an analogy to socket which requests a connection pipe from a
 * {@link JslipcPipeServer}. Method {@link #connect(Class...)} requests a new
 * connection and waits for the server response.
 */
public class JslipcPipeClient implements TimeoutAware {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(JslipcPipeClient.class);
	
	private File serverDirectory;
	private int timeout = 0;


	
	/**
	 * Creates a client talking to the {@link JslipcPipeServer} on the host
	 * directory. See {@link JslipcPipeServer#getConnectDir(HostDir)}.
	 * 
	 * @param hostDir
	 *            the directory hosting the connect and pipes dir.
	 * @throws IOException
	 */
	public JslipcPipeClient(final HostDir hostDir) throws IOException {
		this(JslipcPipeServer.getConnectDir(hostDir));
	}
	
	/**
	 * Creates a client talking to the {@link JslipcPipeServer} on the given
	 * directory.
	 * 
	 * @param serverDirectory
	 *            the JslipcPipeServer directory.
	 * @throws IOException
	 */
	public JslipcPipeClient(final File serverDirectory) throws IOException {
		if (serverDirectory == null) {
			throw new IllegalArgumentException(
					"parameter 'serverDirectory' must not be null");
		}
		if (!serverDirectory.exists()) {
			throw new IOException(serverDirectory.getAbsolutePath()
					+ " does not exist");
		}
		if (!serverDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					serverDirectory.getAbsolutePath() + " is not a directory");
		}
		this.serverDirectory = serverDirectory;
		
		LOGGER.debug("created {} on server directory {}", this.getClass().getSimpleName(), serverDirectory);
	}

	/**
	 * @return the server directory.
	 */
	public File getServerDirectory() {
		return serverDirectory;
	}
	
	/**
	 * Requests and waits for a pipe created by the corresponding
	 * {@link JslipcPipeServer}
	 * 
	 * @param acceptedTypes
	 *            the pipe types accepted by the client.
	 * @return the created pipe.
	 * @throws IOException
	 */
	public JslipcPipe connect(Class<? extends JslipcPipe>... acceptedTypes)
			throws IOException {
		JslipcRequest request = createRequest(acceptedTypes);
		return connect(request);
	}

	/**
	 * Requests and waits for a pipe created by the corresponding
	 * {@link JslipcPipeServer}
	 * 
	 * @param request
	 *            the request to send.
	 * @return the created pipe.
	 * @throws IOException
	 */
	public JslipcPipe connect(final JslipcRequest request) throws IOException {
		LOGGER.debug("connecting to server {}", serverDirectory);
		File directory = FileUtil.createDirectory(serverDirectory);
		FilePipe connectPipe = new FilePipe(directory, JslipcRole.Yang);
		connectPipe.cleanUpOnClose();

		JslipcChannelOutputStream out = new JslipcChannelOutputStream(
				connectPipe.sink());
		out.setTimeout(getTimeout());
		sendRequest(out, request);

		JslipcChannelInputStream in = new JslipcChannelInputStream(
				connectPipe.source());
		in.setTimeout(getTimeout());
		JslipcPipe pipe = readResponse(in);
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman) pipe).cleanUpOnClose();
		}
		LOGGER.debug("established pipe {}", pipe);

		connectPipe.close();
		return pipe;
	}

	/**
	 * Writes a request for a pipe to the given OutputStream.
	 * 
	 * @param out
	 *            the stream to write to.
	 * @param request
	 *            the request to write.
	 * @throws IOException
	 */
	protected void sendRequest(final OutputStream out, final JslipcRequest request)
			throws IOException {
		LOGGER.debug("sending request {} to server {}", request, serverDirectory);
		byte[] bytes = request.toBytes();
		out.write(bytes);
		out.flush();
		out.close();
	}

	/**
	 * Creates a JslipcRequest for the given accept-types.
	 * 
	 * @param acceptedTypes
	 * @return the create request
	 * @throws IOException
	 */
	protected JslipcRequest createRequest(
			Class<? extends JslipcPipe>... acceptedTypes) throws IOException {
		JslipcRequest request = new JslipcRequest(JslipcCommand.CONNECT);
		request.setAcceptTypes(acceptedTypes);
		return request;
	}

	/**
	 * Reads the response from the server and creates the pipe.
	 * 
	 * @param in
	 *            the stream to read from.
	 * @return the created pipe.
	 * @throws IOException
	 */
	protected JslipcPipe readResponse(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();

		JslipcResponse response = new JslipcResponse(baos.toByteArray());
		LOGGER.debug("read response {} from server {}", response, serverDirectory);
		
		if (response.getCode() != JslipcCode.PipeCreated) {
			throw new IOException("connect failed,  " + response.getCode()
					+ ", " + response.getMessage());
		}
		Class<? extends JslipcPipe> type = response.getTypeParameter();
		if (FilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JslipcResponse.PARAM_DIRECTORY);
			JslipcRole role = getRole(response);
			return new FilePipe(dir, role);
		}
		if (ChunkFilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JslipcResponse.PARAM_DIRECTORY);
			JslipcRole role = getRole(response);
			return new ChunkFilePipe(dir, role);
		}
		if (SharedMemoryPipe.class.equals(type)) {
			File file = response.getFileParameter(JslipcResponse.PARAM_FILE);
			JslipcRole role = getRole(response);
			Integer size = response.getIntParameter(JslipcResponse.PARAM_SIZE);
			if (size != null) {
				return new SharedMemoryPipe(file, size, role);
			}
			return new SharedMemoryPipe(file, role);
		}
		throw new IOException("unknown type '" + type + "'");
	}

	private JslipcRole getRole(JslipcResponse response) throws IOException {
		try {
			return JslipcRole.valueOf(response
					.getParameter(JslipcResponse.PARAM_ROLE));
		} catch (Exception e) {
			throw new IOException("unkown role '"
					+ response.getParameter(JslipcResponse.PARAM_ROLE) + "'");
		}
	}

	/**
	 * @return the connect timeout in ms.
	 */
	@Override
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sets the connect-timeout of the client.
	 * @param timeout the timeout in ms.
	 */
	@Override
	public void setTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("parameter timeout must be > 0: " + timeout);
		}
		this.timeout = timeout;
	}

	
	@Override
	public String toString() {
		return StringUtil.build(this).add("server", serverDirectory).add("timeout", timeout).toString();
	}

}
