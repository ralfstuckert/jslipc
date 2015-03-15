# Java Socket-Less Inter-Process Communication API #
The goal of the jslipc project is to provide Java APIs for inter-process communication (IPC) as an alternative to sockets.

So what's wrong with sockets? Nowadays most clients (at least on windows) are running anti-virus software, local firewalls and all other kinds of security stuff. But usually they prevent opening sockets :-\

There are alternatives to sockets, e.g. using shared memory or files. This is, where jslipc is heading to. There are already many Java IPC libraries out there , but these are either thin wrappers around sockets (why would I need that?) or orphans, started as a couple of lines of code, left alone in the dark. Let's see if we can do better...

**Jslipc is now available in the [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cjslipc)**
<br />
## [![](https://ssl.gstatic.com/codesite/ph/images/dl_arrow.gif)](https://code.google.com/p/jslipc/wiki/Downloads#Version_0.2.3) [Version 0.2.3](Downloads#Version_0.2.3.md) ##

**Fixes**
  * [Issue 18](https://code.google.com/p/jslipc/issues/detail?id=18): Rename ServerDir to a less misleading name
  * [Issue 19](https://code.google.com/p/jslipc/issues/detail?id=19): Support for ServerDir in JSlipPipeServer and -Client
  * [Issue 20](https://code.google.com/p/jslipc/issues/detail?id=20): Add examples for using timeouts

