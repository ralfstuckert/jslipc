package org.jslipc.ipc.pipe.file;
import java.io.File;

import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.PipeProducer;
import org.jslipc.ipc.pipe.file.FilePipe;


public class FilePipeProducer {

	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JslipcRole.Yin);
		
		PipeProducer pipeProducer = new PipeProducer();
		pipeProducer.talkToConsumer(pipe);
	}

}
