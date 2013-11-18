package org.jslipc.channel;

import java.nio.channels.WritableByteChannel;

/**
 * Marker interface that integrates {@link WritableByteChannel} and {@link JslipcChannel}.
 */
public interface WritableJslipcByteChannel extends JslipcChannel,
		WritableByteChannel {

}
