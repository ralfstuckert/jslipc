<img src="https://github.com/ralfstuckert/jslipc/blob/master/org.jslipc/javadoc/resources/jslipcLogo.png" alt="Logo" style="height: 80px;"/>
#Java Socket-Less Inter-Process Communication API#

*currently moving from [googlecode](https://code.google.com/p/jslipc/) to github, not yet complete...*

The goal of the jslipc project is to provide Java APIs for inter-process communication (IPC) as an alternative to sockets. 

So what's wrong with sockets? Nowadays most clients (at least on windows) are running anti-virus software, local firewalls and all other kinds of security stuff. But usually they prevent opening sockets :-\ 

There are alternatives to sockets, e.g. using shared memory or files. This is, where jslipc is heading to. There are already many Java IPC libraries out there , but these are either thin wrappers around sockets (why would I need that?) or orphans, started as a couple of lines of code, left alone in the dark. Let's see if we can do better...

__Jslipc is now available in the [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cjslipc)__

##<a href="https://github.com/ralfstuckert/jslipc/releases/tag/jslipc-0.2.3">Version 0.2.3</a>##

**Fixes**
- [Issue #18](https://github.com/ralfstuckert/jslipc/issues/18): Rename ServerDir to a less misleading name
- [Issue #19](https://github.com/ralfstuckert/jslipc/issues/19): Support for ServerDir in JSlipPipeServer and -Client
- [Issue #20](https://github.com/ralfstuckert/jslipc/issues/20): Add examples for using timeouts

 
