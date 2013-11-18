package org.jslipc.ipc.pipe.file;

import java.io.File;

import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.PipeConsumer;
import org.jslipc.ipc.pipe.file.FilePipe;

public class FilePipeConsumer {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JslipcRole.Yang);

		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.talkToProducer(pipe);
	}

}
