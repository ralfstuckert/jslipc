package org.jslipc.ipc.pipe;

import java.net.URL;
import java.net.URLConnection;

public class URLClient {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		URL url = new URL("jslipc://./server/connect?accept-type=ChunkFilePipe");
		URLConnection connection = url.openConnection();

		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.talkToProducer(connection.getOutputStream(), connection.getInputStream());
	}

}
