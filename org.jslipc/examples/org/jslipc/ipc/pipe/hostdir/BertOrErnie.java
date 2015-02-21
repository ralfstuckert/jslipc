package org.jslipc.ipc.pipe.hostdir;

import java.io.File;
import java.io.IOException;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.Bert;
import org.jslipc.ipc.pipe.Ernie;
import org.jslipc.ipc.pipe.JslipcConnection;
import org.jslipc.ipc.pipe.JslipcPipeClient;
import org.jslipc.ipc.pipe.JslipcPipeServer;
import org.jslipc.ipc.pipe.JslipcPipeClient.DirectoryType;
import org.jslipc.util.ActiveHostException;
import org.jslipc.util.HostDir;

public class BertOrErnie {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File sharedDir = new File("./server");
		sharedDir.mkdir();
		try {
			actAsBert(sharedDir);
		} catch (ActiveHostException e) {
			actAsErnie(sharedDir);
		}
	}

	private static void actAsBert(File sharedDir) throws IOException {
		HostDir hostDir = HostDir.create(sharedDir);
		JslipcPipeServer server = new JslipcPipeServer(hostDir);
		// setup timeouts
		server.setAcceptTimeout(10000); // timeout for accepting new
										// connections
		server.setTimeout(10000); // timeout for an incoming connection to
									// proceed

		// accept connection
		JslipcConnection connection = server.accept();
		JslipcPipe pipe = connection.getPipe();

		Bert bert = new Bert();
		bert.talkToErnie(pipe);
	}

	private static void actAsErnie(File sharedDir) throws IOException {
		JslipcPipeClient client = new JslipcPipeClient(sharedDir,
				DirectoryType.Host);
		// setup timeouts
		client.setTimeout(10000); // connect-timeout
		// request connection
		@SuppressWarnings("unchecked")
		JslipcPipe pipe = client.connect();

		Ernie ernie = new Ernie();
		ernie.talkToBert(pipe);
	}

}
