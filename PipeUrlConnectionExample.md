A lot of intermediate communication protocols and APIs are based on URLConnection. In order to support them, Jslipc provides an URLConnection and an appropriate protocol handler. Like an HttpURLConnection connects to an HTTP Server, the JslipcURLConnection will connect to an JslipcPipeServer. The server/Bert-part is quite the same as in the PipeServerExample, so we wil just show the significant Ernie part here.

Ernie just sets up an URL with the protocol _jslipc_ and the path of the server connection directory. He also specifies, what kind of pipe(s) he will accept, but this is optional. After that, he just uses the URLConnection's streams to talk to Bert:

```
public class ErnieWithURLConnection {
	
	public static void main(String[] args) throws Exception {
		new File("./server/connect").mkdirs();

		// set up pipe
		URL url = new URL("jslipc://./server/connect?accept-types=ChunkFilePipe");
		URLConnection connection = url.openConnection();

		Ernie ernie = new Ernie();
		ernie.talkToBert(connection.getOutputStream(), connection.getInputStream());
	}
}
```