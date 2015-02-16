package org.jslipc.ipc.pipe.hostdir;

import java.net.URL;
import java.net.URLConnection;

import org.jslipc.ipc.pipe.Ernie;

public class ErnieWithURLConnection {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		URL url = new URL("jslipc:hostdir://./server?accept-types=ChunkFilePipe");
		URLConnection connection = url.openConnection();

		Ernie ernie = new Ernie();
		ernie.talkToBert(connection.getOutputStream(), connection.getInputStream());
	}

}
