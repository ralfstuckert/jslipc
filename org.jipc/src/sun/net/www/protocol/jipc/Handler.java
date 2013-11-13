package sun.net.www.protocol.jipc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jipc.JipcPipe;
import org.jipc.ipc.pipe.JipcPipeClient;
import org.jipc.ipc.pipe.JipcPipeURLConnection;
import org.jipc.ipc.pipe.JipcRequest;
import org.jipc.ipc.pipe.JipcRequest.JipcCommand;
import org.jipc.util.StringUtil;

/**
 * Handles <code>jipc</code> protocol URLs. The syntax is
 * 
 * <pre>
 * jipc:///&lt;server-directory-path&gt;[?param1=value1&amp; ... &amp;paramx=valuex]
 * </pre>
 * 
 * Examples:
 * 
 * <pre>
 * jipc:///c:/example/server/connect
 * jipc:///c:/example/server/connect?accept-types=ChunkFilePipe,FilePipe
 * </pre>
 * 
 * So any parameter can be passed as part of the query.
 */
public class Handler extends URLStreamHandler {

	protected File serverConnectDirectory;
	
	@Override
	protected void parseURL(URL url, String spec, int start, int limit) {
		
		int startIndex = spec.indexOf("://") + 3;
		if (startIndex < start) {
			startIndex = 0;
		}
		int endIndex = spec.indexOf('?');
		if (endIndex == -1) {
			endIndex = spec.length();
		}
		serverConnectDirectory = new File(spec.substring(startIndex, endIndex));
		super.parseURL(url, spec, start, limit);
		
	}
	
	@Override
	protected URLConnection openConnection(final URL url) throws IOException {
		JipcPipeClient client = new JipcPipeClient(serverConnectDirectory);
		JipcRequest request = createRequest(url);
		JipcPipe pipe = client.connect(request);
		return new JipcPipeURLConnection(url, pipe);
	}

	/**
	 * Creates the request from the given URL.
	 * 
	 * @param url
	 * @return the created request.
	 * @throws IOException
	 */
	protected JipcRequest createRequest(final URL url) throws IOException {
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
