package org.jipc.ipc.pipe;

import java.io.File;

import org.jipc.JipcPipe;
import org.jipc.ipc.pipe.JipcPipeClient;
import org.jipc.ipc.pipe.file.Consumer;

public class Client {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./server/connect");
		directory.mkdirs();
		JipcPipeClient client = new JipcPipeClient(directory);
		// request connection
		JipcPipe pipe = client.connect(JipcPipeClient.ALL_PIPES);

		Consumer consumer = new Consumer();
		consumer.talkToProducer(pipe);
	}

}
