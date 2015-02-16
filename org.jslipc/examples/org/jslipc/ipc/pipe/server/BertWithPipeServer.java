package org.jslipc.ipc.pipe.server;

import java.io.File;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.Bert;
import org.jslipc.ipc.pipe.JslipcConnection;
import org.jslipc.ipc.pipe.JslipcPipeServer;

public class BertWithPipeServer {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File serverDir = new File("./server");
		serverDir.mkdir();
		File connectDir = new File(serverDir, "connect");
		connectDir.mkdir();
		File pipeDir = new File(serverDir, "pipe");
		pipeDir.mkdir();
		JslipcPipeServer server = new JslipcPipeServer(connectDir, pipeDir);

		// accept connection
		JslipcConnection connection = server.accept();
		JslipcPipe pipe = connection.getPipe();
		
		Bert bert = new Bert();
		bert.talkToErnie(pipe);
	}

}
