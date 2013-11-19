package org.jslipc.ipc.pipe.file;
import java.io.File;

import org.jslipc.JslipcRole;
import org.jslipc.ipc.pipe.Bert;
import org.jslipc.ipc.pipe.file.FilePipe;


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
