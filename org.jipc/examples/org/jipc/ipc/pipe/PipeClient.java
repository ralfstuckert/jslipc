package org.jipc.ipc.pipe;

import java.io.File;

import org.jipc.JipcPipe;

public class PipeClient {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./server/connect");
		directory.mkdirs();
		JipcPipeClient client = new JipcPipeClient(directory);
		// request connection
		@SuppressWarnings("unchecked")
		JipcPipe pipe = client.connect();

		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.talkToProducer(pipe);
	}

}
