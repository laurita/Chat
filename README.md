Chat
====

A simple chat application written in Scala using Akka actor model.

Server
------

A server side of the application.

sbt run-main ChatServerApp 'server_port'

NewClient
---------

A client side of the appliction.

sbt run-main CmdLineClientApp 'server_host' 'server_port'