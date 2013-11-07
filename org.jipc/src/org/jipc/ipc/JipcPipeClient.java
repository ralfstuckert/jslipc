package org.jipc.ipc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.channel.file.FileUtil;
import org.jipc.ipc.JipcRequest.JipcCommand;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;

public class JipcPipeClient {

	private File serverDirectory;

	public JipcPipeClient(final File serverDirectory) throws IOException {
		if (serverDirectory == null) {
			throw new IllegalArgumentException(
					"parameter 'serverDirectory' must not be null");
		}
		if (!serverDirectory.exists()) {
			throw new IllegalArgumentException(
					serverDirectory.getAbsolutePath() + " does not exist");
		}
		if (!serverDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					serverDirectory.getAbsolutePath() + " is not a directory");
		}
		this.serverDirectory = serverDirectory;
	}

	public JipcPipe connect(Class<? extends JipcPipe>... acceptedTypes)
			throws IOException {
		File directory = FileUtil.createDirectory(serverDirectory);
		FilePipe pipe = new FilePipe(directory, JipcRole.Yang);
		pipe.cleanUpOnClose();

		requestPipe(new JipcChannelOutputStream(pipe.sink()), acceptedTypes);
		JipcPipe response = waitForResponse(new JipcChannelInputStream(
				pipe.source()));

		pipe.close();
		return response;
	}

	protected void requestPipe(final OutputStream out,
			Class<? extends JipcPipe>... acceptedTypes) throws IOException {
		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);
		request.setAcceptTypes(acceptedTypes);
		byte[] bytes = request.toBytes();
		out.write(bytes);
		out.flush();
		out.close();
	}

	protected JipcPipe waitForResponse(final InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in,
				StandardCharsets.UTF_8));
		String status = reader.readLine();
		String[] split = status.split(" ");
		if (split.length != 2) {
			throw new IOException("bad response '" + status + "'");
		}

		int code = Integer.parseInt(split[0]);
		String protocol = split[1];
		if (!"JIPC/1.0".equals(protocol)) {
			throw new IOException("unknown protocol '" + protocol + "'");
		}
		if (code != 200) {
			throw new IOException("connect failed, code is " + code);
		}

		Map<String, String> parameter = parseParameter(reader);

		String type = parameter.get("TYPE");
		if (getTypeName(FilePipe.class).equals(type)) {
			File dir = getFileParameter(parameter, "DIRECTORY");
			JipcRole role = getRole(parameter);
			return new FilePipe(dir, role);
		}
		if (getTypeName(ChunkFilePipe.class).equals(type)) {
			File dir = getFileParameter(parameter, "DIRECTORY");
			JipcRole role = getRole(parameter);
			return new ChunkFilePipe(dir, role);
		}
		if (getTypeName(SharedMemoryPipe.class).equals(type)) {
			File file = getFileParameter(parameter, "FILE");
			JipcRole role = getRole(parameter);
			Integer size = getIntParameter(parameter, "SIZE");
			if (size != null) {
				return new SharedMemoryPipe(file, size, role);
			}
			return new SharedMemoryPipe(file, role);
		}
		throw new IOException("unknown type '" + type + "'");
	}

	private File getFileParameter(Map<String, String> parameter, String key)
			throws IOException, UnsupportedEncodingException {
		String directory = parameter.get(key);
		if (directory == null) {
			throw new IOException(
					"bad protocol, parameter DIRECTORY is missing '"
							+ parameter + "'");
		}
		directory = URLDecoder.decode(directory,
				StandardCharsets.UTF_8.toString());
		File dir = new File(directory);
		return dir;
	}

	private JipcRole getRole(Map<String, String> parameter) throws IOException {
		try {
			return JipcRole.valueOf(parameter.get("ROLE"));
		} catch (Exception e) {
			throw new IOException("unkown role '" + parameter.get("ROLE") + "'");
		}
	}

	private Integer getIntParameter(Map<String, String> parameter, String key) {
		try {
			return Integer.valueOf(parameter.get(key));
		} catch (Exception e) {
			return null;
		}
	}

	private Map<String, String> parseParameter(BufferedReader reader)
			throws IOException {
		Map<String, String> result = new HashMap<String, String>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			int index = line.indexOf(':');
			if (index > 0 && index < line.length() - 1) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				result.put(key, value);
			}
		}
		return result;
	}

	protected String getTypeName(final Class<? extends JipcPipe> pipeClass) {
		return pipeClass.getSimpleName();
	}
}
