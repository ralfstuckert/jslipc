package org.jslipc.ipc.pipe.hostdir;

import java.io.File;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.Ernie;
import org.jslipc.ipc.pipe.JslipcPipeClient;
import org.jslipc.ipc.pipe.JslipcPipeClient.DirectoryType;

public class ErnieWithPipeClient {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File sharedDir = new File("./server");
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
