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

public class PipeProducer {

	public void talkToConsumer(final JipcPipe pipe) throws IOException {
		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JipcChannelOutputStream(pipe.sink());
		InputStream in = new JipcChannelInputStream(pipe.source());

		talkToConsumer(out, in);

		if (pipe instanceof JipcBinman) {
			((JipcBinman)pipe).close();
		}
	}

	public void talkToConsumer(OutputStream out, InputStream in)
			throws IOException {
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
	}

}
