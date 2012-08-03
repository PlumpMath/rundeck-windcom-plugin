Rundeck DCOM Plugin
--------------------

This is a [Rundeck Node Execution plugin][1] that uses DCOM to connect to Windows and execute commands.  It uses [j-Interop][2] to provide the DCOM implementation.

[1]: http://rundeck.org/docs/manual/plugins.html#node-execution-plugins
[2]: //http://j-interop.org

Install
====

Copy the `rundeck-dcom-plugin-1.0.jar` to the `libext/` directory for Rundeck.

Configure The Plugin
====

Configure a Windows Server for DCOM 
====

Build
=====

Build with gradle

Prerequisites: the `rundeck-core-1.4.x.jar` file should be placed in the lib directory.

Gradle build, result is `build/libs/rundeck-windcom-plugin-1.0.jar`.

	gradle clean build


Getting the Rundeck core jar
====	

If you're building the plug-in independently of Rundeck itself you can find the Rundeck core jar in a couple of places under your Rundeck install. e.g:

	[rundeck@centos62 ~]$ find . -name rundeck-core-\*.jar
	./cli/rundeck-core-1.4.4-dev.jar
	./exp/webapp/WEB-INF/lib/rundeck-core-1.4.4-dev.jar
