The Jslipc API supports various blocking operations: streams waiting on a read or write, clients waiting to connect to the server, server waiting for an incoming connection a.s.o.

Since it is not a good idea to wait indefinitely on blocking operations, Jslipc supports timeouts for all blocking operations. In order to ease up finding all locations where you may specify a timeout, an interface TimeoutAware was introduced. It just defines two methods 'getTimeout()' and 'setTimeout()'.

Lets pimp up our [Ernie and Bert](Examples.md) example classes with timeouts:

```
        ...
        // set up streams
        JslipcChannelOutputStream out = new JslipcChannelOutputStream(pipe.sink());
        JslipcChannelInputStream in = new JslipcChannelInputStream(pipe.source());

        // setup timeouts
        out.setTimeout(10000); // 10 seconds
        in.setTimeout(10000); // 10 seconds
        ...
```

[JSlipcPipeClient and -Server](PipeServerExample.md) do also support (read/write-) timeouts. But since the are an analogy to Socket and ServerSocket, they additionally have connect- resp. accept-timeouts:

```
public class BertWithPipeServer {
        ...
        JslipcPipeServer server = new JslipcPipeServer(connectDir, pipeDir);
        // setup timeouts
        server.setAcceptTimeout(10000); // timeout for accepting new connections
        server.setTimeout(10000); // timeout for an incoming connection to proceed
        ...
```

```
public class ErnieWithPipeClient {
        ...
        JslipcPipeClient client = new JslipcPipeClient(directory);
        // setup timeouts
        client.setTimeout(10000); // connect-timeout
        ...
```

The connect- and read-timeout defined by the java URLConnection class is also supported in [Jslipc PipeURLConnection](PipeUrlConnectionExample.md):

```
public class ErnieWithURLConnection {
        ...
        URL url = new URL("jslipc://./server/connect?accept-types=ChunkFilePipe");
        URLConnection connection = url.openConnection();
        // setup timeouts
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        ...
```