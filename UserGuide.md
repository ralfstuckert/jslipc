# User Guide #
The Jipc project provides APIs for easy inter-process communication using well know Java-APIs like Channels and Streams. The central interface is `JslipcPipe`:

```
public interface JslipcPipe {

	/**
	 * @return the channel to read from.
	 * @throws IOException 
	 */
	ReadableJslipcByteChannel source() throws IOException;

	/**
	 * @return the channel to write to.
	 * @throws IOException 
	 */
	WritableJslipcByteChannel sink() throws IOException;
}
```

The pipe just provides a source and sink channel. Most pipes - except for the [SharedMemoryPipe](#SharedMemoryPipe.md) - are not blocking, so the channels won't block either. But sometimes it is comfortable to have blocking operations, and that's what the `JipcChannelInputStream` resp. the `JipcChannelOutputStream` is good for. These stream classes also support timeouts, so you don't have to wait endlessly. The following sections will describe the available pipe types and their properties. Look at the [examples page](Examples.md) for some usage scenarios.

## FilePipe ##
This is - technically - the simplest of all pipes. Two files are used for communication, one for each channel. Writing to the channel means appending to the file, where the reader just reads the file front of. It is easy to handle, and not slower than regular file I/O. The price is, that the file grows on every write and does not shrink on reads. So it is not the perfect choice for long conversations. The FilePipe is used internally by the JslipPipeServer and -Client for connection set up. The FilePipe may be created by providing the needed two files, or a directory (where to create the files in) and the role of the endpoint. The roles yin and yang do not have any special semantics, the two endpoints of the pipe just must have complementary roles. The usage is shown in the FilePipeExample.

## ChunkFilePipe ##
The ChunkFilePipe's idea is to eliminate the [FilePipe's](#FilePipe.md) drawback to waste disk space. The ChunkFilePipe uses a directory for each of the two pipe channels. Every time a chunk of data is written to the channel, a file is created in the channel directory containing that data. If a chunk is read by the reader, this (consumed) chunk file is removed. Means reading the channel releases disk space. The set up of the ChunkFilePipe is similar two [FilePipe](#FilePipe.md), means you can either specify the two needed directories, or a directory and role which is used to set up the two directories.

## SharedMemoryPipe ##
As the name says, shared memory is used for communication between the pipe's endpoints. The shared memory is set up mapping a file into memory. In this piece of memory, two bounded buffers are established which represent the two channels. Once mapped, this pipe is quite fast. But since the buffers are bounded, this pipe may block on reads to an empty buffer, resp. writes to a full buffer.

## JslipcFileServer and -Client ##
The JslipcFileServer was introduced as an equivalent to a ServerSocket (see [Issue#9](https://code.google.com/p/jslipc/issues/detail?id=#9)). The JslipcFileServer's accept() method "listens" for incomning request on a given connection-directory. Once a requst comes in, it sets up a new pipe in a separate (pipe-) directory and returns it. This pipe can now be used to serve the client.

The JslipcClient on the other side connects the server - using the given connection-directory - and returns the established pipe. Optionally, you may specify which kinds of pipes you are willing to accept. So the client is some kind of counterpart to the java Socket class. This kind of client-server communication is show in the [PipeServerExample](PipeServerExample.md).

## URLConnection ##
Since many communication APIs are based on URLConnections, Jslipc also provides a way to set up a connection to a [JslipcServer](#JslipcFileServer_and_-Client.md) using a simple URL protocol (see [Issue#11](https://code.google.com/p/jslipc/issues/detail?id=#11)). All you got to so is use the `jslipc` protocol and the connection-directory path. As with the client, you can also specify which kinds of pipes you are willing to accept. You can find a usage szenario in the [PipeUrlConnectionExample](PipeUrlConnectionExample.md).

## Timeouts ##
Timeouts are essential when dealing with blocking operations. Therefore Jslipc has built in support for timeouts in streams and all client/server constructs. See the [timeout examples](Timeouts.md) for some common usage scenarios.

## HostDir ##
When you are dealing with client/server architectures, you need some kind of address shared by client and server in order to set up communication. With Jslipc, this shared information is usually a directory. But in order to (re-)use always the same file/directory, you must provide appropriate clean up, which can be quite a pain. The only way to cope with that problem, is to always use a new file or directory. Here is were HostDir comes to the rescue. This utility class also helps you in scenarios, where multiple instances of your application have to come to an agreement on which one is the server. This is explained in [First one serves](FirstOneServes.md).