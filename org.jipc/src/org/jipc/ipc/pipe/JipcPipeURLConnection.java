package org.jipc.ipc.pipe;

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

import org.jipc.JipcPipe;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.util.StringUtil;

/**
 * This is an {@link URLConnection} based on a {@link JipcPipe}.
 */
public class JipcPipeURLConnection extends URLConnection {

	protected JipcPipeClient client;
	protected InputStream in;
	protected OutputStream out;
	
	/**
	 * Create an URL connection based on the given pipe.
	 * @param url the URL passed to the super class.
	 * @param client the client used to create the streams.
	 */
	public JipcPipeURLConnection(final URL url, final JipcPipeClient client) throws IOException {
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

		JipcRequest request = createRequest();
		client.setTimeout(getConnectTimeout());
		JipcPipe pipe = client.connect(request);
		
		JipcChannelInputStream jipcInputStream = new JipcChannelInputStream(pipe.source());
		jipcInputStream.setTimeout(getReadTimeout());
		in = new BufferedInputStream(jipcInputStream);

		JipcChannelOutputStream jipcOutputStream = new JipcChannelOutputStream(pipe.sink());
		jipcOutputStream.setTimeout(getReadTimeout());
		out = new BufferedOutputStream(jipcOutputStream);
		
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
	protected JipcRequest createRequest() throws IOException {
		JipcRequest request = new JipcRequest(JipcCommand.CONNECT);

		Map<String, String> parameters = parseParameter(url.getQuery());
		if (parameters != null) {
			for (Entry<String, String> entry : parameters.entrySet()) {
				request.setParameter(entry.getKey(), entry.getValue());
			}
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
