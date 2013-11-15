package sun.net.www.protocol.jipc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jipc.ipc.pipe.JipcPipeClient;
import org.jipc.ipc.pipe.JipcPipeURLConnection;

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
		return new JipcPipeURLConnection(url, client);
	}

}
