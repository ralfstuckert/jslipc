package org.jipc.ipc.pipe;

import java.io.File;

import org.jipc.JipcPipe;

public class PipeServer {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File serverDir = new File("./server");
		serverDir.mkdir();
		File connectDir = new File(serverDir, "connect");
		connectDir.mkdir();
		File pipeDir = new File(serverDir, "pipe");
		pipeDir.mkdir();
		JipcPipeServer server = new JipcPipeServer(connectDir, pipeDir);

		// accept connection
		JipcConnection connection = server.accept();
		JipcPipe pipe = connection.getPipe();
		
		PipeProducer pipeProducer = new PipeProducer();
		pipeProducer.talkToConsumer(pipe);
	}

}
