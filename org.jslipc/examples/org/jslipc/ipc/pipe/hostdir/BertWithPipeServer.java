package org.jslipc.ipc.pipe.hostdir;

import java.io.File;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.Bert;
import org.jslipc.ipc.pipe.JslipcConnection;
import org.jslipc.ipc.pipe.JslipcPipeServer;
import org.jslipc.util.HostDir;

public class BertWithPipeServer {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File serverDir = new File("./server");
		serverDir.mkdir();
		HostDir hostDir = HostDir.create(serverDir);
		JslipcPipeServer server = new JslipcPipeServer(hostDir);
		// setup timeouts
		server.setAcceptTimeout(10000);
		server.setTimeout(10000);

		// accept connection
		JslipcConnection connection = server.accept();
		JslipcPipe pipe = connection.getPipe();
		
		Bert bert = new Bert();
		bert.talkToErnie(pipe);
	}

}
