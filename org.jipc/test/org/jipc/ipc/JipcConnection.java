package org.jipc.ipc;

import java.util.Map;

import org.jipc.JipcPipe;

/**
 * This is a container for a pipe and the request parameters.
 */
public class JipcConnection {

	private JipcPipe pipe;
	private Map<String,String> parameter;

	/**
	 * @param pipe
	 * @param parameter
	 */
	public JipcConnection(JipcPipe pipe, Map<String, String> parameter) {
		super();
		this.pipe = pipe;
		this.parameter = parameter;
	}
	
	/**
	 * @return the pipe.
	 */
	public JipcPipe getPipe() {
		return pipe;
	}
	
	/**
	 * @return the request parameter.
	 */
	public Map<String, String> getRequestParameters() {
		return parameter;
	}
	
	
}
