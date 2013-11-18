package org.jslipc.ipc.pipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.jslipc.JslipcBinman;
import org.jslipc.JslipcPipe;
import org.jslipc.channel.JslipcChannelInputStream;
import org.jslipc.channel.JslipcChannelOutputStream;

public class PipeProducer {

	public void talkToConsumer(final JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		OutputStream out = new JslipcChannelOutputStream(pipe.sink());
		InputStream in = new JslipcChannelInputStream(pipe.source());

		talkToConsumer(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
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
