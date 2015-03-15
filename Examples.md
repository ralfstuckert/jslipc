The Jipc project provides APIs for easy inter-process communication using well know Java-APIs like Channels and Streams. The following code snippets demonstrate the usage of various kinds of pipes.

The scenario in these examples is always the same: [Ernie and Bert](http://en.wikipedia.org/wiki/Bert_and_Ernie) are talking to each other using a pipe. Let's see Bert's part first. Bert uses a pipe to set up In- and OutputStreams, and uses them to talk to Ernie. He sends _"Hi Ernie, how are you?"_, and waits for a reply from Ernie:

```
public class Bert {

	public void talkToErnie(final JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JslipcChannelOutputStream(pipe.sink());
		InputStream in = new JslipcChannelInputStream(pipe.source());

		talkToErnie(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
		}
	}

	public void talkToErnie(OutputStream out, InputStream in)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

		System.out.println("This is Bert sendinng a message to Ernie.");
		// send message to ernie
		System.out.println("I'm sending: 'Hi Ernie, how are you?'");
		writer.write("Hi Ernie, how are you?\n");
		writer.flush();
		// receive message from ernie
		String received = reader.readLine();
		System.out.println("Ernie replied: '" + received + "'\n");

		// close all resources
		reader.close();
		writer.close();
	}

}
```

Ernie's setup is quite similar, except that he first waits for the message from Bert, and replies _"I'm fine"_:

```
public class Ernie {

	public void talkToBert(JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JslipcChannelOutputStream(pipe.sink());
		InputStream in = new JslipcChannelInputStream(pipe.source());

		talkToBert(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
		}
	}

	public void talkToBert(OutputStream out, InputStream in)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		System.out.println("This is Ernie waiting for a message from Bert.");
		// receive message from producer
		String received = reader.readLine();
		System.out.println("Bert sent: '" + received + "'" );
		// send message to producer
		System.out.println("I will reply: 'I'm fine'" );
		writer.write("I'm fine\n");

		// close all resources
		reader.close();
		writer.close();
	}
}
```

So after the two processes have finished, you'll see the following output on their consoles:

**Bert's console:**
```
This is Bert sendinng a message to Ernie.
I'm sending: 'Hi Ernie, how are you?'
Ernie replied: 'I'm fine'
```
**Ernie's console:**
```
This is Ernie waiting for a message from Bert.
Bert sent: 'Hi Ernie, how are you?'
I will reply: 'I'm fine'
```

The following examples will demonstrate this usage scenario using different kinds of pipes:

  * **FilePipeExample**
  * **PipeServerExample**
  * **PipeUrlConnectionExample**