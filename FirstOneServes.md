# First one serves #

Suppose you have an application, where multiple instances may run at the same time, and those instances want to communicate. The question is: which one is the server? The Jslipc HostDir functionality allows to implement a first-one-serves strategy, means: the first instance starts the server, and the others are clients. But how does the instance know whether it is the first one? The `HostDir.create()` supports such concurrency scenarios: if there is currently no active server, the call succeeds and you can start your server. If not, you'll get an `ActiveHostException` telling you that there is already a server instance running... so you may act as a client.

Sounds weird? Let's do it by example with our [Ernie-Bert](Examples.md) conversation. We will implement the class `BertOrErnie`, which tries to act as Bert, means: it tries to set up a server: If this fails, it will assume that there is already a Bert (server), and act as Ernie (client). Just run this class twice and watch both instance act as Bert resp. as Ernie.

```
public class BertOrErnie {

    public static void main(String[] args) throws Exception {
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
        JslipcPipe pipe = client.connect();

        Ernie ernie = new Ernie();
        ernie.talkToBert(pipe);
    }

}
```