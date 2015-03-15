The **PipeServer** example demonstrate the scenario sketched in the [examples overview](Examples.md) using a JslipcPipeServer and -Client. The PipeServer is an analogy to ServerSocket, so it is designed to accept and handle multiple requests for setting up a new pipe. In our Ernie/Bert scenario only one pipe is set up, so using a server may be a lil' oversized ;-)

Both Ernie and Bert run in their own process. Let's see Bert first. Bert creates the server connection- and pipe-directory, waits for an incoming pipe request, and uses the pipe to talk to Ernie:

```
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
```

Ernie's part is a bit different. He also creates the connection directory (that's necessary if the Client - Ernie - is started before the server), and create a JslipcPipClient. After that he uses the client to connect to the server, and starts talking to Bert via the pipe:

```
public class ErnieWithPipeClient {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./server/connect");
		directory.mkdirs();
		JslipcPipeClient client = new JslipcPipeClient(directory);
		// request connection
		JslipcPipe pipe = client.connect();

		Ernie ernie = new Ernie();
		ernie.talkToBert(pipe);
	}

}
```