<h1>Java Socket-Less Inter-Process Communication API</h1>

<em>currently moving from <a href="https://code.google.com/p/jslipc/">googlecode</a> to github, not yet complete...</em>

The goal of the jslipc project is to provide Java APIs for inter-process communication (IPC) as an alternative to sockets. 

So what's wrong with sockets? Nowadays most clients (at least on windows) are running anti-virus software, local firewalls and all other kinds of security stuff. But usually they prevent opening sockets :-\ 

There are alternatives to sockets, e.g. using shared memory or files. This is, where jslipc is heading to. There are already many Java IPC libraries out there , but these are either thin wrappers around sockets (why would I need that?) or orphans, started as a couple of lines of code, left alone in the dark. Let's see if we can do better...

<strong>Jslipc is now available in the <a href="http://search.maven.org/#search%7Cga%7C1%7Cjslipc">Maven Central Repository</a></strong>
<br/>
<h2><a href="https://code.google.com/p/jslipc/wiki/Downloads#Version_0.2.3"><img href="https://ssl.gstatic.com/codesite/ph/images/dl_arrow.gif" /> Version 0.2.3</a> </h2>

<strong>Fixes</strong>
<ul>
<li>Issue 18: Rename !ServerDir to a less misleading name</li>
<li>Issue 19: Support for !ServerDir in JSlipPipeServer and -Client</li>
<li>Issue 20: Add examples for using timeouts</li>
</ul>
 
 
