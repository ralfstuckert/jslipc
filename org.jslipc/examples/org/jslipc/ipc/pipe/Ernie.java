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

public class Ernie {

	public void talkToBert(JslipcPipe pipe) throws IOException {
		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).cleanUpOnClose();
		}
		// set up streams
		JslipcChannelOutputStream out = new JslipcChannelOutputStream(pipe.sink());
		JslipcChannelInputStream in = new JslipcChannelInputStream(pipe.source());

		// setup timeouts
		out.setTimeout(10000); // 10 seconds
		in.setTimeout(10000); // 10 seconds

		talkToBert(out, in);

		if (pipe instanceof JslipcBinman) {
			((JslipcBinman)pipe).close();
		}
	}

	public void talkToBert(OutputStream out, InputStream in)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		
		System.out.println("This is Ernie waiting for a message from Bert.");
		// receive message from producer
		String received = reader.readLine();
		System.out.println("Bert sent: '" + received + "'" );
		// send message to producer
		System.out.println("I will reply: 'I'm fine'" );
		writer.write("I'm fine\n");

		// close all resources
		reader.close();
		writer.close();
	}
}
