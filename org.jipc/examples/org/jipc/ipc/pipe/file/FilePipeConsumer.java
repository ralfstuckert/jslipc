package org.jipc.ipc.pipe.file;

import java.io.File;

import org.jipc.JipcRole;
import org.jipc.ipc.pipe.PipeConsumer;

public class FilePipeConsumer {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JipcRole.Yang);

		PipeConsumer pipeConsumer = new PipeConsumer();
		pipeConsumer.talkToProducer(pipe);
	}

}
