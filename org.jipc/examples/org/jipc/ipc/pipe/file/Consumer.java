package org.jipc.ipc.pipe.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;
import org.jipc.JipcRole;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;
import org.jipc.ipc.pipe.file.FilePipe;

public class Consumer {
	
	public static void main(String[] args) throws Exception {
		// set up pipe
		File directory = new File("./pipe");
		directory.mkdir();
		FilePipe pipe = new FilePipe(directory, JipcRole.Yang);

		Consumer consumer = new Consumer();
		consumer.talkToProducer(pipe);
	}

	public void talkToProducer(JipcPipe pipe) throws IOException {
		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JipcChannelOutputStream(pipe.sink());
		InputStream in = new JipcChannelInputStream(pipe.source());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		// receive message from producer
		String received = reader.readLine();
		System.out.println("producer asked: '" + received + "'" );
		// send message to producer
		System.out.println("answering: 'I'm fine'" );
		writer.write("I'm fine\n");

		// close all resources
		reader.close();
		writer.close();
		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).close();
		}
	}
}
