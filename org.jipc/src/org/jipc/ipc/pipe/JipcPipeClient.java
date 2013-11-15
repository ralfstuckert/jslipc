package org.jipc.ipc.pipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jipc.JipcBinman;
import org.jipc.TimeoutAware;
import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.ipc.pipe.JipcResponse.JipcCode;
import org.jipc.ipc.pipe.file.ChunkFilePipe;
import org.jipc.ipc.pipe.file.FilePipe;
import org.jipc.ipc.pipe.shm.SharedMemoryPipe;
import org.jipc.util.FileUtil;

/**
 * This is an analogy to socket which requests a connection pipe from a
 * {@link JipcPipeServer}. Method {@link #connect(Class...)} requests a new
 * connection and waits for the server response.
 */
public class JipcPipeClient implements TimeoutAware {

	private File serverDirectory;
	private int timeout = 0;

	/**
	 * Creates a client talking to the {@link JipcPipeServer} on the given
	 * directory.
	 * 
	 * @param serverDirectory
	 *            the JipcPipeServer directory.
	 * @throws IOException
	 */
	public JipcPipeClient(final File serverDirectory) throws IOException {
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
	}

	/**
	 * Requests and waits for a pipe created by the corresponding
	 * {@link JipcPipeServer}
	 * 
	 * @param acceptedTypes
	 *            the pipe types accepted by the client.
	 * @return the created pipe.
	 * @throws IOException
	 */
	public JipcPipe connect(Class<? extends JipcPipe>... acceptedTypes)
			throws IOException {
		JipcRequest request = createRequest(acceptedTypes);
		return connect(request);
	}

	/**
	 * Requests and waits for a pipe created by the corresponding
	 * {@link JipcPipeServer}
	 * 
	 * @param request
	 *            the request to send.
	 * @return the created pipe.
	 * @throws IOException
	 */
	public JipcPipe connect(final JipcRequest request) throws IOException {
		File directory = FileUtil.createDirectory(serverDirectory);
		FilePipe connectPipe = new FilePipe(directory, JipcRole.Yang);
		connectPipe.cleanUpOnClose();

		JipcChannelOutputStream out = new JipcChannelOutputStream(
				connectPipe.sink());
		out.setTimeout(getTimeout());
		sendRequest(out, request);

		JipcChannelInputStream in = new JipcChannelInputStream(
				connectPipe.source());
		in.setTimeout(getTimeout());
		JipcPipe pipe = readResponse(in);
		if (pipe instanceof JipcBinman) {
			((JipcBinman) pipe).cleanUpOnClose();
		}

		connectPipe.close();
		return pipe;
	}

	/**
	 * Writes a request for a pipe to the given OutputStream.
	 * 
	 * @param out
	 *            the stream to write to.
	 * @param acceptedTypes
	 *            the pipe types accepted by the client.
	 * @throws IOException
	 */
	protected void sendRequest(final OutputStream out, final JipcRequest request)
			throws IOException {
		byte[] bytes = request.toBytes();
		out.write(bytes);
		out.flush();
		out.close();
	}

	/**
	 * Creates a JipcRequest for the given accept-types.
	 * 
	 * @param acceptedTypes
	 * @return the create request
	 * @throws IOException
	 */
	protected JipcRequest createRequest(
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
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
	protected JipcPipe readResponse(final InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int date = 0;
		while ((date = in.read()) != -1) {
			baos.write(date);
		}
		in.close();

		JipcResponse response = new JipcResponse(baos.toByteArray());
		if (response.getCode() != JipcCode.PipeCreated) {
			throw new IOException("connect failed,  " + response.getCode()
					+ ", " + response.getMessage());
		}
		Class<? extends JipcPipe> type = response.getTypeParameter();
		if (FilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new FilePipe(dir, role);
		}
		if (ChunkFilePipe.class.equals(type)) {
			File dir = response.getFileParameter(JipcResponse.PARAM_DIRECTORY);
			JipcRole role = getRole(response);
			return new ChunkFilePipe(dir, role);
		}
		if (SharedMemoryPipe.class.equals(type)) {
			File file = response.getFileParameter(JipcResponse.PARAM_FILE);
			JipcRole role = getRole(response);
			Integer size = response.getIntParameter(JipcResponse.PARAM_SIZE);
			if (size != null) {
				return new SharedMemoryPipe(file, size, role);
			}
			return new SharedMemoryPipe(file, role);
		}
		throw new IOException("unknown type '" + type + "'");
	}

	private JipcRole getRole(JipcResponse response) throws IOException {
		try {
			return JipcRole.valueOf(response
					.getParameter(JipcResponse.PARAM_ROLE));
		} catch (Exception e) {
			throw new IOException("unkown role '"
					+ response.getParameter(JipcResponse.PARAM_ROLE) + "'");
		}
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("parameter timeout must be > 0: " + timeout);
		}
		this.timeout = timeout;
	}

}
