package org.jslipc.ipc.pipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jslipc.JslipcPipe;
import org.jslipc.util.StringUtil;

/**
 * Encapsulates the request sent by the {@link JslipcPipeClient}.
 */
public class JslipcRequest extends AbstractJslipcMessage {

	public static enum JslipcCommand {
		CONNECT;
	}

	private JslipcCommand command;

	/**
	 * Creates a JslipcRequest from its byte array representation.
	 * @see #toBytes()
	 */
	public JslipcRequest(final byte[] request) throws IOException {
		super(new String(request, StandardCharsets.UTF_8));
	}

	/**
	 * Creates a JslipcRequest from its string representation.
	 * @see #toString()
	 */
	public JslipcRequest(final String request) throws IOException {
		super(request);
	}

	/**
	 * Creates a JslipcRequest with the given commad.
	 */
	public JslipcRequest(final JslipcCommand command) throws IOException {
		super(command + " " + JSLIPC_PROTOCOL_PREFIX + "1.0");
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
			command = JslipcCommand.valueOf(parts[0]);
		} catch (Exception e) {
			throw new IOException("bad request header: '" + header + "'");
		}
		return parts[1];
	}

	/**
	 * @return the command of the request.
	 */
	public JslipcCommand getCommand() {
		return command;
	}

	/**
	 * Sets the pipe types to accept.
	 * @param acceptedTypes
	 */
	public void setAcceptTypes(final Class<? extends JslipcPipe>... acceptedTypes) {
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
	public List<Class<? extends JslipcPipe>> getAcceptTypes() {
		List<Class<? extends JslipcPipe>> result = new ArrayList<Class<? extends JslipcPipe>>();
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
