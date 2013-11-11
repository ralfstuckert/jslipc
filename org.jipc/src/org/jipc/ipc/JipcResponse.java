package org.jipc.ipc;

import java.io.IOException;

import org.jipc.JipcRole;

/**
 * Encapsulates the response sent by the {@link JipcPipeServer}.
 */
public class JipcResponse extends AbstractJipcMessage {

	/**
	 * The code indicates the result of the operation.
	 */
	public enum JipcCode {
		PipeCreated(200), BadRequest(400), InternalError(500);

		private int value;

		private JipcCode(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static JipcCode valueOf(int value) {
			for (JipcCode code : JipcCode.values()) {
				if (code.value() == value) {
					return code;
				}
			}
			throw new IllegalArgumentException("JipcCode value '" + value
					+ "' is not supported");
		}
	}

	private JipcCode code;
	private String message;

	/**
	 * Creates a JipcResponse from its byte array representation.
	 * @see #toBytes()
	 */
	public JipcResponse(final byte[] response) throws IOException {
		super(response);
	}

	/**
	 * Creates a JipcResponse from its string representation.
	 * @see #toString()
	 */
	public JipcResponse(final String response) throws IOException {
		super(response);
	}
	
	/**
	 * Creates a JipcResponse with the given code and message.
	 */
	public JipcResponse(final JipcCode code, final String message) throws IOException {
		super(JIPC_PROTOCOL_PREFIX + "1.0 " + code.value() + " " + message);
	}
	
	@Override
	protected String getMessageName() {
		return "responest";
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

	private JipcCode getCode(final String value, final String header)
			throws IOException {
		try {
			int code = Integer.valueOf(value.trim());
			return JipcCode.valueOf(code);
		} catch (Exception e) {
			throw new IOException("bad header: '" + header + "'");
		}
	}

	public JipcCode getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * @return the value of the parameter {@link #PARAM_ROLE}.
	 */
	public JipcRole getRoleParameter() {
		return JipcRole.valueOf(getParameter(PARAM_ROLE));
	}
	
	/**
	 * @return the value of the parameter {@link #PARAM_ROLE}.
	 */
	public void setRoleParameter(final JipcRole role) {
		setParameter(PARAM_ROLE, role.toString());
	}
	
}
