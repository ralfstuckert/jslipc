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
		File sharedDir = new File("./server");
		sharedDir.mkdir();
		HostDir hostDir = HostDir.create(sharedDir);
		JslipcPipeServer server = new JslipcPipeServer(hostDir);
		// setup timeouts
		server.setAcceptTimeout(10000); // timeout for accepting new connections
		server.setTimeout(10000); // timeout for an incoming connection to proceed

		// accept connection
		JslipcConnection connection = server.accept();
		JslipcPipe pipe = connection.getPipe();
		
		Bert bert = new Bert();
		bert.talkToErnie(pipe);
	}

}
