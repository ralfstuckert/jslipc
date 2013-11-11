package org.jipc.ipc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jipc.JipcPipe;
import org.jipc.StringUtil;
import org.jipc.UrlUtil;
import org.jipc.ipc.file.ChunkFilePipe;
import org.jipc.ipc.file.FilePipe;
import org.jipc.ipc.shm.SharedMemoryPipe;

/**
 * Abstract base class for both {@link JipcRequest} and {@link JipcResponse}.
 */
public abstract class AbstractJipcMessage {

	public static final String PARAM_DIRECTORY = "DIRECTORY";
	public static final String PARAM_FILE = "FILE";
	public static final String PARAM_ROLE = "ROLE";
	public static final String PARAM_TYPE = "TYPE";
	public static final String PARAM_SIZE = "SIZE";
	public static final String PARAM_ACCEPT_TYPES = "ACCEPT-TYPES";

	protected static final String JIPC_PROTOCOL_PREFIX = "JIPC/";
	protected static final String UTF_8 = StandardCharsets.UTF_8.toString();
	@SuppressWarnings("unchecked")
	protected static final List<Class<? extends JipcPipe>> SUPPORTED_PIPES = Arrays
			.asList((Class<? extends JipcPipe>) FilePipe.class,
					(Class<? extends JipcPipe>) ChunkFilePipe.class,
					(Class<? extends JipcPipe>) SharedMemoryPipe.class);

	private String protocolVersion;
	private Map<String, String> parameter = new HashMap<String, String>();

	/**
	 * Creates a message from its byte array representation.
	 * 
	 * @see #toBytes()
	 */
	public AbstractJipcMessage(final byte[] message) throws IOException {
		this(new String(message, StandardCharsets.UTF_8));
	}

	/**
	 * Creates a message from its string representation.
	 * 
	 * @see #toString()
	 */
	public AbstractJipcMessage(final String message) throws IOException {
		parseMessage(message);
	}
	
	/**
	 * @return either 'request' or 'response'.
	 */
	protected abstract String getMessageName();

	/**
	 * Parses the message header and parameter.
	 */
	protected void parseMessage(final String message) throws IOException {
		List<String> lines = StringUtil.splitLines(message);
		if (lines.size() < 1) {
			throw new IOException("bad " + getMessageName() + " '" + message + "'");
		}

		String protocolPart = parseHeader(lines.get(0));
		if (!protocolPart.startsWith(JIPC_PROTOCOL_PREFIX)
				|| protocolPart.length() == JIPC_PROTOCOL_PREFIX.length()) {
			throw new IOException("bad protocol: '" + protocolPart + "'");
		}
		protocolVersion = protocolPart.substring(JIPC_PROTOCOL_PREFIX.length());

		if (lines.size() > 1) {
			parseParameter(lines.subList(1, lines.size()));
		}
	}

	/**
	 * Parses the header line of the message.
	 * 
	 * @param header
	 * @return the protocol part of the header
	 * @throws IOException
	 */
	protected abstract String parseHeader(String header) throws IOException;

	private void parseParameter(List<String> lines) throws IOException {
		for (String line : lines) {
			int index = line.indexOf(':');
			if (index > 0 && index < line.length() - 1) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				value = UrlUtil.urlDecode(value);
				parameter.put(key, value);
			}
		}
	}

	/**
	 * @return all parameters.
	 */
	public Map<String,String> getParameters() {
		return Collections.unmodifiableMap(parameter);
	}
	
	/**
	 * Returns the parameter or <code>null</code> if not found.
	 * 
	 * @param key
	 * @return the parameter or <code>null</code>.
	 */
	public String getParameter(final String key) {
		return parameter.get(key);
	}

	/**
	 * Returns the value of the parameter as a File, or <code>null</code> if not
	 * found.
	 * 
	 * @param key
	 * @return the File or <code>null</code>.
	 */
	public File getFileParameter(final String key) {
		String value = getParameter(key);
		if (value == null) {
			return null;
		}
		return new File(value);
	}

	/**
	 * Sets the given File parameter.
	 * 
	 * @param key
	 * @param file
	 */
	public void setFileParameter(final String key, final File file) {
		if (file == null) {
			setParameter(key, null);
		} else {
			setParameter(key, file.getAbsolutePath());
		}
	}

	/**
	 * Returns the integer value of the parameter, or <code>null</code> if not
	 * found.
	 * 
	 * @param key
	 * @return the integer value or <code>null</code>.
	 * @throws IOException
	 *             if the given value is not an integer.
	 */
	public Integer getIntParameter(final String key) throws IOException {
		String value = getParameter(key);
		if (value == null) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new IOException("expected numerical value for '" + key
					+ "' but is '" + value + "'");
		}
	}

	/**
	 * Sets the given integer parameter.
	 * 
	 * @param key
	 * @param integer
	 */
	public void setIntParameter(final String key, final Integer integer)
			throws IOException {
		if (integer == null) {
			setParameter(key, null);
		} else {
			try {
				setParameter(key, Integer.toString(integer));
			} catch (NumberFormatException e) {
				throw new IOException("expected numerical value for '" + key
						+ "' but is '" + integer + "'");
			}
		}
	}

	/**
	 * @return the value of the parameter {@link #PARAM_TYPE}.
	 */
	public Class<? extends JipcPipe> getTypeParameter() {
		return getTypeClass(getParameter(PARAM_TYPE));
	}
	
	/**
	 * @return the value of the parameter {@link #PARAM_TYPE}.
	 */
	public void setTypeParameter(final Class<? extends JipcPipe> type) {
		setParameter(PARAM_TYPE, getTypeName(type));
	}
	
	protected String getTypeName(final Class<? extends JipcPipe> pipeClass) {
		return pipeClass.getSimpleName();
	}
	
	protected Class<? extends JipcPipe> getTypeClass(final String type) {
		if (type == null) {
			return null;
		}
		for (Class<? extends JipcPipe> clazz : SUPPORTED_PIPES) {
			if (type.equals(getTypeName(clazz))) {
				return clazz;
			}
		}
		return null;
	}
	/**
	 * @param key
	 * @return <code>true</code> if the given parameter exists.
	 */
	public boolean hasParameter(final String key) {
		return parameter.containsKey(key);
	}

	/**
	 * Adds a parameter.
	 * 
	 * @param key
	 * @param value
	 */
	public void setParameter(final String key, final String value) {
		parameter.put(key, value);
	}

	/**
	 * @return the command of the message.
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * @return the complete protocol identifier, e.g. <code>JIPC/1.0</code>.
	 */
	public String getProtocol() {
		return JIPC_PROTOCOL_PREFIX + getProtocolVersion();
	}

	/**
	 * @return the header of the message.
	 */
	protected abstract String getHeader();

	public String toString() {
		StringBuilder bob = new StringBuilder();
		bob.append(getHeader());
		bob.append('\n');
		for (Entry<String, String> entry : parameter.entrySet()) {
			bob.append(entry.getKey());
			bob.append(": ");
			if (entry.getValue() != null) {
				bob.append(UrlUtil.urlEncode(entry.getValue()));
			}
			bob.append('\n');
		}
		return bob.toString();
	}

	public byte[] toBytes() {
		return toString().getBytes(StandardCharsets.UTF_8);
	}

}
