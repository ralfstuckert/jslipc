package org.jslipc.channel;

import java.nio.channels.Channel;

/**
 * Common interface for all channels.
 */
public interface JslipcChannel extends Channel {

	/**
	 * See {@link JslipcChannel#getState()}.
	 */
	enum JslipcChannelState {
		/**
		 * Channel is open.
		 */
		Open,
		/**
		 * Channel is closed by peer, but this end of the channel is still open.
		 * You may be still able to read from this channel.
		 */
		ClosedByPeer,
		/**
		 * Channel is closed.
		 */
		Closed;
	}
	
	/**
	 * @return the {@link JslipcChannelState state} of the channel.
	 */
	JslipcChannelState getState();
}
