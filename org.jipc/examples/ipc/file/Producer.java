package ipc.file;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.ipc.file.FilePipe;


public class Producer {
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JipcRole.Yin);
		pipe.cleanUpOnClose();
		// set up streams
		OutputStream out = new JipcChannelOutputStream(pipe.sink());
		InputStream in = new JipcChannelInputStream(pipe.source());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		// send message to consumer
		System.out.println("asking consumer: 'How are you?'");
		writer.write("How are you?\n");
		writer.flush();
		// receive message from consumer
		String received = reader.readLine();
		System.out.println("consumer answered: '" + received + "'\n");

		// close all resources
		reader.close();
		writer.close();
		pipe.close();
	}

}
