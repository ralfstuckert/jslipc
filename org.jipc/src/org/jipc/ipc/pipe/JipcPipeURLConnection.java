package org.jipc.ipc.pipe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jipc.JipcPipe;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;

/**
 * This is an {@link URLConnection} based on a {@link JipcPipe}.
 */
public class JipcPipeURLConnection extends URLConnection {

	protected JipcPipe pipe;
	protected InputStream in;
	protected OutputStream out;
	
	/**
	 * Create an URL connection based on the given pipe.
	 * @param url the URL passed to the super class.
	 * @param pipe the pipe used to create the streams.
	 */
	public JipcPipeURLConnection(URL url, JipcPipe pipe) {
		super(url);
		if (url == null) {
			throw new IllegalArgumentException("parameter '" + url + "' must not be null");
		}
		if (pipe == null) {
			throw new IllegalArgumentException("parameter '" + pipe + "' must not be null");
		}
		this.pipe = pipe;
	}


	@Override
	public synchronized void connect() throws IOException {
		if (connected) {
			return;
		}
		in = new BufferedInputStream(new JipcChannelInputStream(pipe.source()));
		out = new BufferedOutputStream(new JipcChannelOutputStream(pipe.sink()));
		
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

}
