package org.jslipc.ipc.pipe;

import java.io.File;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.JslipcPipeClient;

public class PipeClient {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./server/connect");
		directory.mkdirs();
		JslipcPipeClient client = new JslipcPipeClient(directory);
		// request connection
		@SuppressWarnings("unchecked")
		JslipcPipe pipe = client.connect();

		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.talkToProducer(pipe);
	}

}
