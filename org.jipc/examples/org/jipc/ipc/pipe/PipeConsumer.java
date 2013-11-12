package org.jipc.ipc.pipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jipc.JipcBinman;
import org.jipc.JipcPipe;
import org.jipc.channel.JipcChannelInputStream;
import org.jipc.channel.JipcChannelOutputStream;

public class PipeConsumer {

	public void talkToProducer(JipcPipe pipe) throws IOException {
		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JipcChannelOutputStream(pipe.sink());
		InputStream in = new JipcChannelInputStream(pipe.source());

		talkToProducer(out, in);

		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).close();
		}
	}

	public void talkToProducer(OutputStream out, InputStream in)
			throws IOException {
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
	}
}
