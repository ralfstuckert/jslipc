package org.jipc.channel;

import java.nio.channels.ReadableByteChannel;

/**
 * Marker interface that integrates {@link ReadableByteChannel} and {@link JipcChannel}.
 */
public interface ReadableJipcByteChannel extends ReadableByteChannel, JipcChannel {

}
