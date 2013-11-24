package org.jslipc.ipc.pipe;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;

public class ErnieWithURLConnection {
	
	public static void main(String[] args) throws Exception {
		new File("./server/connect").mkdirs();

		// set up pipe
		URL url = new URL("jslipc://./server/connect?accept-types=ChunkFilePipe");
		URLConnection connection = url.openConnection();

		Ernie ernie = new Ernie();
		ernie.talkToBert(connection.getOutputStream(), connection.getInputStream());
	}

}
