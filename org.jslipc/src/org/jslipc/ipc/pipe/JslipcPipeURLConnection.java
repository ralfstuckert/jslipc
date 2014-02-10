package org.jslipc.ipc.pipe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jslipc.JslipcPipe;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;
import org.jslipc.ipc.pipe.JslipcRequest.JslipcCommand;
import org.jslipc.util.StringUtil;

/**
 * This is an {@link URLConnection} based on a {@link JslipcPipe}.
 */
public class JslipcPipeURLConnection extends URLConnection {

	protected JslipcPipeClient client;
	protected InputStream in;
	protected OutputStream out;
	
	/**
	 * Create an URL connection based on the given pipe.
	 * @param url the URL passed to the super class.
	 * @param client the client used to create the streams.
	 */
	public JslipcPipeURLConnection(final URL url, final JslipcPipeClient client) throws IOException {
		super(url);
		if (url == null) {
			throw new IllegalArgumentException("parameter 'url' must not be null");
		}
		if (client == null) {
			throw new IllegalArgumentException("parameter 'client' must not be null");
		}
		this.client = client;
	}


	@Override
	public synchronized void connect() throws IOException {
		if (connected) {
			return;
		}

		JslipcRequest request = createRequest();
		client.setTimeout(getConnectTimeout());
		JslipcPipe pipe = client.connect(request);
		
		JslipcChannelInputStream inputStream = new JslipcChannelInputStream(pipe.source());
		inputStream.setTimeout(getReadTimeout());
		in = new BufferedInputStream(inputStream);

		JslipcChannelOutputStream outputStream = new JslipcChannelOutputStream(pipe.sink());
		outputStream.setTimeout(getReadTimeout());
		out = new BufferedOutputStream(outputStream);
		
		connected = true;
	}
	
	@Override
	public synchronized InputStream getInputStream() throws IOException {
		if (!connected) {
			connect();
		}
		return in;
	}
	
	@Override
	public synchronized OutputStream getOutputStream() throws IOException {
		if (!connected) {
			connect();
		}
		return out;
	}

	/**
	 * Creates the request from the given URL.
	 * 
	 * @param url
	 * @return the created request.
	 * @throws IOException
	 */
	protected JslipcRequest createRequest() throws IOException {
		JslipcRequest request = new JslipcRequest(JslipcCommand.CONNECT);

		// pass query parameter
		Map<String, String> parameters = parseParameter(url.getQuery());
		if (parameters != null) {
			for (Entry<String, String> entry : parameters.entrySet()) {
				request.setParameter(entry.getKey(), entry.getValue());
			}
		}
		
		// pass request properties
		for (Entry<String, List<String>> entry : getRequestProperties().entrySet()) {
			request.setParameter(entry.getKey(), StringUtil.join(entry.getValue()));
		}
		
		return request;
	}

	private Map<String, String> parseParameter(String query) {
		Map<String, String> result = new HashMap<String, String>();
		if (query != null) {
			List<String> parameters = StringUtil.split(query, '&');
			for (String keyValue : parameters) {
				int index = keyValue.indexOf('=');
				if (index > 0 && index < keyValue.length() - 2) {
					String key = keyValue.substring(0, index);
					String value = keyValue.substring(index + 1);
					result.put(key, value);
				}
			}
		}
		return result;
	}

}
