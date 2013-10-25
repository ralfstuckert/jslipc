package org.jipc.channel;

import java.nio.channels.Channel;

/**
 * Common interface for all channels.
 */
public interface JipcChannel extends Channel {

	/**
	 * See {@link JipcChannel#getState()}.
	 */
	enum JipcChannelState {
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
	 * @return the {@link JipcChannelState state} of the channel.
	 */
	JipcChannelState getState();
}
