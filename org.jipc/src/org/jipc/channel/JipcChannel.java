package org.jipc.channel;

import java.nio.channels.Channel;

public interface JipcChannel extends Channel {

	boolean isClosedByPeer();
}
