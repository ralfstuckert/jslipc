When you are dealing with client/server architectures, you need some kind of address shared by client and server in order to set up communication. With sockets, this is the host address resp. the port number. Jslipc is file based, the shared information here is either a file or a directory.

But in order to (re-)use always the same file/directory, you must provide appropriate clean up, otherwise you may get in conflict with old communication traces. Jslipc [Binman](Binman.md) gives it best to clean up old data when a stream or pipe is closed. But there is no way to make this safe. Even if you close all streams and releases all files, there are chances that you cannot delete a file... a common problem on windows platforms.

The only way to cope with that problem, is to always use a **new** file or directory. But how does a client know, which directory to use, when the server creates a new one every time. That's where HomeDir comes into play (see [issue #17](https://code.google.com/p/jslipc/issues/detail?id=#17)). This utility allows client and server to use ONE directory as the shared information needed to set up a client server pipeline.

Method `create(File)` tries to create a new (sub-) directory in the directory shared by client and server. The name of this directory is hold in a special file called `host.lock`, which is locked by the host. The created directory will be the active host directory as long as the host process lives (holds the file lock), or the HostDir is closed.

```
    HostDir hostDir = HostDir.create(sharedDir);
    File activeHostDir = hostDir.getDirectory();
```

If you try to create a new HostDir while there is already an active one, you will receive a ActiveHostException. This exception provides you the directory of the currently active host. Using that mechanism, you can also set up multi-process scenarios where the every process tries to be the server. If it gets an exception, it will know that there is already an active server and act as a client.

The client on the other side uses method `getActive()` to get acive (sub-) directory currently in use by the server:

```
    File activeHostDir = HostDir.getActive(sharedDir);
```

So the only information client and server need to know up front is the `sharedDir`, the active server dir itself is created resp. determined at runtime. This allows an easy and safe way to deal with clean and fresh directories.

The [JslipcPipeServer and -Client](PipeServerExample.md) have built in support for `HostDir`. Just provide the HostDir resp. the shared directory, and the corresponding connect directory is determined and created automatically. Let's see our `BertWithPipeServer` with HostDir:

```
public class BertWithPipeServer {
    ...
    // set up pipe
    File sharedDir = new File("./server");
    sharedDir.mkdir();
    HostDir hostDir = HostDir.create(sharedDir);
    JslipcPipeServer server = new JslipcPipeServer(hostDir);
    ...
```

And now the corresponding `ErnieWithPipeClient`. In order to distiguish between the given directory being a host directory or the concrete server directory, the client is also handed a directory type:

```
public class ErnieWithPipeClient {
    ...
    // set up pipe
    File sharedDir = new File("./server");
    JslipcPipeClient client = new JslipcPipeClient(sharedDir, DirectoryType.Host);
    ...
```

The last exammple with HostDir support is our [JslipcPipeURLConnection](PipeUrlConnectionExample.md). A slight change in the protocol gives Jslipc the hint, that the given directory is a HostDir:

```
public class ErnieWithURLConnection {
    ...
    // set up pipe
    URL url = new URL("jslipc:hostdir://./server?accept-types=ChunkFilePipe");
    URLConnection connection = url.openConnection();
    ...
```