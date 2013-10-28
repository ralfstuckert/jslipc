package org.jipc.channel;

import java.nio.channels.WritableByteChannel;

/**
 * Marker interface that integrates {@link WritableByteChannel} and {@link JipcChannel}.
 */
public interface WritableJipcByteChannel extends JipcChannel,
		WritableByteChannel {

}
