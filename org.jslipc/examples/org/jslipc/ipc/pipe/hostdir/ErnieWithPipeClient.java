package org.jslipc.ipc.pipe.hostdir;

import java.io.File;

import org.jslipc.JslipcPipe;
import org.jslipc.ipc.pipe.Ernie;
import org.jslipc.ipc.pipe.JslipcPipeClient;
import org.jslipc.util.PipeUtil;

public class ErnieWithPipeClient {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File serverDir = new File("./server");
		serverDir.mkdir();
		File connectDir = PipeUtil.getActiveHostConnectDir(serverDir);
		
		JslipcPipeClient client = new JslipcPipeClient(connectDir);
		// request connection
		@SuppressWarnings("unchecked")
		JslipcPipe pipe = client.connect();

		Ernie ernie = new Ernie();
		ernie.talkToBert(pipe);
	}

}
