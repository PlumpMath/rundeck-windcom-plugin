Rundeck DCOM Plugin
--------------------

This is a [Rundeck Node Execution plugin][1] that uses DCOM to connect to Windows and execute commands.  The plugin is based on the [j-Interop][2] open source library making cross-platform operations possible (i.e. you can run commands on Windows boxes from a non-Windows Rundeck server).

The NodeExecutor service is implemented using the WshShell (WScript.Shell) Windows Script Host (WSH) COM object's Exec method to [run Windows programs][3], capture their output and error streams and report their exit code.

[1]: http://rundeck.org/docs/manual/plugins.html#node-execution-plugins
[2]: http://j-interop.org
[3]: http://technet.microsoft.com/en-us/library/ee156605.aspx

Install
====

Copy the `rundeck-dcom-plugin-1.0.jar` to the `libext/` directory for Rundeck.

Configure The Plugin
====

Generate the encrypted password:

     [rundeck@centos62 libext]$ java -classpath rundeck-windcom-plugin.jar com.dtolabs.rundeck.plugin.windcom.CryptUtil /tmp/key.file Administrator '!1qazxsw2@'

     [INFO]: file: "/tmp/key.file" created.
     
     [INFO]: place the following in your project "resource.xml" file:
     <node ... key-file-path="/tmp/key.file"
               username="Administrator"
               password="tIt3fAtgAgLjY7CcIJMVwHIikwiA/WP1CBcPM9qAwbw=" .../>
     
     <?xml version="1.0" encoding="UTF-8"?>

Setup the node entry:

     <project>
       <node name="localhost" description="Rundeck server node" tags="" hostname="localhost" osArch="amd64" osFamily="unix" osName="Linux" osVersion="2.6.32-220.el6.x86_64" username="rundeck"/>
       <node name="win2008" description="Windows 2008 server node" tags="" hostname="win2008" osArch="" osFamily="" osName="" osVersion="" username="Administrator" domain="win2008" password="tIt3fAtgAgLjY7CcIJMVwHIikwiA/WP1CBcPM9qAwbw=" key-file-path="/tmp/key.file"/>
     </project>

Configure a Windows Server for DCOM 
====

Enabling DCOM access varies depending on the version of Windows you're using. This [Technet article][1] applies to the more recent releases. This article on [Configuring DCOM for Remote Access][2] outlines the various authentication levels and security permissions that have to be set.



[1]: http://technet.microsoft.com/en-us/library/cc771387.aspx
[2]: http://j-integra.intrinsyc.com/support/com/doc/remoteaccess.html

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
