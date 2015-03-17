package org.jslipc.ipc.pipe;

import java.io.IOException;

import org.jslipc.JslipcRole;

/**
 * Encapsulates the response sent by the {@link JslipcPipeServer}.
 */
public class JslipcResponse extends AbstractJslipcMessage {

	/**
	 * The code indicates the result of the operation.
	 */
	public enum JslipcCode {
		PipeCreated(200), BadRequest(400), InternalError(500);

		private int value;

		private JslipcCode(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static JslipcCode valueOf(int value) {
			for (JslipcCode code : JslipcCode.values()) {
				if (code.value() == value) {
					return code;
				}
			}
			throw new IllegalArgumentException("JslipcCode value '" + value
					+ "' is not supported");
		}
	}

	private JslipcCode code;
	private String message;

	/**
	 * Creates a JslipcResponse from its byte array representation.
	 * @see #toBytes()
	 */
	public JslipcResponse(final byte[] response) throws IOException {
		super(response);
	}

	/**
	 * Creates a JslipcResponse from its string representation.
	 * @see #toString()
	 */
	public JslipcResponse(final String response) throws IOException {
		super(response);
	}
	
	/**
	 * Creates a JslipcResponse with the given code and message.
	 */
	public JslipcResponse(final JslipcCode code, final String message) throws IOException {
		super(JSLIPC_PROTOCOL_PREFIX + "1.0 " + code.value() + " " + message);
	}
	
	@Override
	protected String getMessageName() {
		return "response";
	}

	@Override
	protected String getHeader() {
		StringBuilder bob = new StringBuilder();
		bob.append(getProtocol());
		bob.append(" ");
		bob.append(Integer.valueOf(getCode().value()));
		bob.append(" ");
		bob.append(getMessage());
		return bob.toString();
	}
	


	@Override
	protected String parseHeader(String header) throws IOException {
		try {
			int index = 0;
			int nextIndex = header.indexOf(' ');
			String protocolPart = header.substring(index, nextIndex);

			index = nextIndex + 1;
			nextIndex = header.indexOf(' ', index);
			code = getCode(header.substring(index, nextIndex), header);

			index = nextIndex + 1;
			nextIndex = header.indexOf(' ', index);
			message = header.substring(index);
			
			return protocolPart;
		} catch (IndexOutOfBoundsException e) {
			throw new IOException("bad response header: '" + header + "'");
		}
	}

	private JslipcCode getCode(final String value, final String header)
			throws IOException {
		try {
			int code = Integer.valueOf(value.trim());
			return JslipcCode.valueOf(code);
		} catch (Exception e) {
			throw new IOException("bad header: '" + header + "'");
		}
	}

	public JslipcCode getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * @return the value of the parameter {@link #PARAM_ROLE}.
	 */
	public JslipcRole getRoleParameter() {
		return JslipcRole.valueOf(getParameter(PARAM_ROLE));
	}
	
	/**
	 * @param role the role parameter to set.
	 */
	public void setRoleParameter(final JslipcRole role) {
		setParameter(PARAM_ROLE, role.toString());
	}
	
}
