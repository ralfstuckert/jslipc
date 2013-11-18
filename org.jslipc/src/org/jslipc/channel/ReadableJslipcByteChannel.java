package org.jslipc.channel;

import java.nio.channels.ReadableByteChannel;

/**
 * Marker interface that integrates {@link ReadableByteChannel} and {@link JslipcChannel}.
 */
public interface ReadableJslipcByteChannel extends ReadableByteChannel, JslipcChannel {

}
