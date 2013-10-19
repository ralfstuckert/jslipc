package org.jipc;

import java.nio.channels.Channel;

public interface JipcChannel extends Channel {

	boolean isClosedByPeer();
}
