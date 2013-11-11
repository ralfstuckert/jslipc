package org.jipc.ipc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jipc.JipcPipe;
import org.jipc.StringUtil;

/**
 * Encapsulates the request sent by the {@link JipcPipeClient}.
 */
public class JipcRequest extends AbstractJipcMessage {

	public static enum JipcCommand {
		CONNECT;
	}

	private JipcCommand command;

	/**
	 * Creates a JipcRequest from its byte array representation.
	 * @see #toBytes()
	 */
	public JipcRequest(final byte[] request) throws IOException {
		super(new String(request, StandardCharsets.UTF_8));
	}

	/**
	 * Creates a JipcRequest from its string representation.
	 * @see #toString()
	 */
	public JipcRequest(final String request) throws IOException {
		super(request);
	}

	/**
	 * Creates a JipcRequest with the given commad.
	 */
	public JipcRequest(final JipcCommand command) throws IOException {
		super(command + " " + JIPC_PROTOCOL_PREFIX + "1.0");
	}
	
	@Override
	protected String getMessageName() {
		return "request";
	}

	@Override
	protected String getHeader() {
		StringBuilder bob = new StringBuilder();
		bob.append(getCommand().toString());
		bob.append(" ");
		bob.append(getProtocol());
		return bob.toString();
	}
	
	/**
	 * Parses the header line of the request.
	 * 
	 * @param header
	 * @throws IOException
	 */
	@Override
	protected String parseHeader(String header) throws IOException {
		String[] parts = header.split(" ");
		if (parts.length < 2) {
			throw new IOException("bad request header: '" + header + "'");
		}
		try {
			command = JipcCommand.valueOf(parts[0]);
		} catch (Exception e) {
			throw new IOException("bad request header: '" + header + "'");
		}
		return parts[1];
	}

	/**
	 * @return the command of the request.
	 */
	public JipcCommand getCommand() {
		return command;
	}

	/**
	 * Sets the pipe types to accept.
	 * @param acceptedTypes
	 */
	public void setAcceptTypes(final Class<? extends JipcPipe>... acceptedTypes) {
		StringBuilder bob = new StringBuilder();
		for (int i = 0; i < acceptedTypes.length; i++) {
			if (i > 0) {
				bob.append(",");
			}
			bob.append(getTypeName(acceptedTypes[i]));
		}
		setParameter(PARAM_ACCEPT_TYPES, bob.toString());
	}
	
	/**
	 * @return the List of accepted pipe types.
	 */
	public List<Class<? extends JipcPipe>> getAcceptTypes() {
		List<Class<? extends JipcPipe>> result = new ArrayList<Class<? extends JipcPipe>>();
		String types = getParameter(PARAM_ACCEPT_TYPES);
		if (types != null) {
			List<String> split = StringUtil.split(types, ',');
			for (String type : split) {
				result.add(getTypeClass(type));
			}
		}
		return result;
	}
	

}
