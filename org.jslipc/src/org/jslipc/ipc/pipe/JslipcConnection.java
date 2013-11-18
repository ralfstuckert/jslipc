package org.jslipc.ipc.pipe;

import java.util.Map;

import org.jslipc.JslipcPipe;

/**
 * This is a container for a pipe and the request parameters.
 */
public class JslipcConnection {

	private JslipcPipe pipe;
	private Map<String,String> parameter;

	/**
	 * @param pipe
	 * @param parameter
	 */
	public JslipcConnection(JslipcPipe pipe, Map<String, String> parameter) {
		super();
		this.pipe = pipe;
		this.parameter = parameter;
	}
	
	/**
	 * @return the pipe.
	 */
	public JslipcPipe getPipe() {
		return pipe;
	}
	
	/**
	 * @return the request parameter.
	 */
	public Map<String, String> getRequestParameters() {
		return parameter;
	}
	
	
}
