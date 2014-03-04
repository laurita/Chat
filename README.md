Chat
====

A simple chat application written in Scala using Akka actor model.

Server
------

A server side of the application.

sbt run-main ChatServerApp 'server_port'

CmdLineClient
-------------

A command line client communicating with server side through TCP.

sbt run-main CmdLineClientApp 'server_host' 'server_port'

WebSocketClient
---------------

An HTTP client using Socko and Web sockets.

sbt run-main WebSocketClientApp

Go to your browser and open 'http://localhost:8888'