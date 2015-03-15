The **FilePipe** example demonstrate the scenario sketched in the [examples overview](Examples.md) using a JslipcFilePipe. Both Ernie and Bert run in their own process. Let's see Bert first. Bert creates a directory, sets up a FilePipe with this directoy and the role Yin, and starts talking using the pipe:

```
public class BertWithFilePipe {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JslipcRole.Yin);
		
		Bert bert = new Bert();
		bert.talkToErnie(pipe);
	}
}
```

Ernie's part is quite similar, but he uses the Yang to set up the pipe. The role does not have special semantics, except that the two endpoints of the pipe must have complementary roles:

```
public class ErnieWithFilePipe {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JslipcRole.Yang);

		Ernie ernie = new Ernie();
		ernie.talkToBert(pipe);
	}
}
```