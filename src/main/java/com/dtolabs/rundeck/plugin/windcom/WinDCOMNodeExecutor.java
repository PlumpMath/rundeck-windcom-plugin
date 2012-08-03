/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * WScriptShellNodeExecutor.java
 * 
 */

package com.dtolabs.rundeck.plugin.windcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.io.IOException;
import java.net.UnknownHostException;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

/**
 * WScriptShellNodeExecutor is ...
 */
@Plugin (name="windcom-node-executor", service = "NodeExecutor")
public class WinDCOMNodeExecutor implements NodeExecutor, Describable {

	public static final String SERVICE_PROVIDER_TYPE = "windcom-node-executor";
	public static final String WINDOWS_NODE_EXEC = "[windcom node executor] ";
	
    public WinDCOMNodeExecutor(final Framework framework) {
    }

    static final List<Property> CONFIG_PROPERTIES = new ArrayList<Property>();
    static final Map<String, String> CONFIG_MAPPING;

    static {
        final Map<String, String> mapping = new HashMap<String, String>();
        CONFIG_MAPPING = Collections.unmodifiableMap(mapping);
    }

    static final Description DESC = new Description() {
        public String getName() {
            return SERVICE_PROVIDER_TYPE;
        }

        public String getTitle() {
            return "WinDCOM";
        }

        public String getDescription() {
            return "Executes a command on a remote Windows node via DCOM.";
        }

        public List<Property> getProperties() {
            return null;
        }

        public Map<String, String> getPropertiesMapping() {
            return null;
        }
    };

    public Description getDescription() {
        return DESC;
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context,
    										 final String[] command,
                                             final INodeEntry node)
      throws ExecutionException {        
        int result = -1;
        DcomService dcomService = null;
		try {
	    	String delimeter = "";
	    	StringBuilder joinedCommandSb = new StringBuilder("cmd /c ");
	    	for (String string : command) {
	    		joinedCommandSb.append(delimeter).append(string);
	    		delimeter = " ";
	    	}

	    	dcomService
			  = new DcomService(context, new WinDCOMNodeAttributes(node));

			StreamListener streamListener = new StreamListener() {
				@Override
				public void stdout(String data) {
					context.getExecutionListener().log(Constants.INFO_LEVEL, data);
				}					

				@Override
				public void stderr(String data) {
					context.getExecutionListener().log(Constants.WARN_LEVEL, data);
				}
			};

			//TODO: Determine timeout correctly!
			dcomService.exec(joinedCommandSb.toString(), 0, streamListener);
			result = dcomService.getExitCode();
		} catch (Exception e) {
	    	throw new ExecutionException(e);
		} finally {
			try {
				if (dcomService != null) {
					dcomService.destroySession();
				}
			} catch (Exception e) {
		        context.getExecutionListener().log(
		          Constants.WARN_LEVEL,
		          String.format("%s (%s:'%s')", WINDOWS_NODE_EXEC,
	          					"error destroying remote DCOM session",
	          					e.getMessage()));
			}
		}

        final int resultCode = result;
        final boolean status = 0 == result;
        return new NodeExecutorResult() {
            public int getResultCode() {
                return resultCode;
            }

            public boolean isSuccess() {
                return status;
            }

            @Override
            public String toString() {
                return WINDOWS_NODE_EXEC + " result was "
                	   + (isSuccess() ? "success" : "failure")
                	   + ", resultcode: " + getResultCode();
            }
        };
    }

	private interface StreamListener {
		public void stdout(String data);
		public void stderr(String data);
	}

    private class DcomService {
    	private final ExecutionContext context;
		private JIComServer comServer = null;
		private IJIDispatch dispatch = null;
		private IJIComObject unknown = null;
		private JISession session = null;
		private int exitCode;
	
		public DcomService(final ExecutionContext context,
						   WinDCOMNodeAttributes windcomNodeAttributes)
		  throws JIException, UnknownHostException {
			// to debug DCOM change this level
			JISystem.getLogger().setLevel(Level.OFF);

			this.context = context;
			
			JISystem.setAutoRegisteration(true);
			session = JISession.createSession(windcomNodeAttributes.getDomainName(),
											  windcomNodeAttributes.getUserName(),
											  windcomNodeAttributes.getPassword());
			comServer = new JIComServer(JIProgId.valueOf("Wscript.Shell"), 
										windcomNodeAttributes.getHostName(), 
										session);
			unknown = comServer.createInstance();
			dispatch = (IJIDispatch)JIObjectFactory.narrowObject(
						 unknown.queryInterface(IJIDispatch.IID));
		}
	
		public void destroySession() throws Exception {
			JISession.destroySession(session);
		}
	
		public void exec(String command, long timeOuMs,
						 StreamListener streamListener) throws Exception {
			StreamThread execThread
			  = new StreamThread(context, command, timeOuMs, streamListener);
			execThread.start();
			execThread.join(timeOuMs);

			if (execThread.getThreadException() != null) {
				throw execThread.getThreadException();
			}

			exitCode = execThread.getExitCode();
		}
		
		public int getExitCode() {
			return exitCode;
		}
	
		private class StreamThread extends Thread {
			private final String command;
			private final long timeOuMs;
			private final StreamListener streamListener;
			private int exitCode;
			private Exception threadException = null;
			private final ExecutionContext context;

			public StreamThread(final ExecutionContext context,
								String command, long timeOuMs,
							    StreamListener streamListener) {
				this.context = context;
				this.command = command;
				this.timeOuMs = timeOuMs;
				this.streamListener = streamListener;
			}
			
			public Exception getThreadException() {
				return threadException;
			}
			
			public int getExitCode() {
				return exitCode;
			}
	
			public void run() {
				IJIDispatch remoteExecObject = null;
				try {
					JIVariant[] jvArr
					  = dispatch.callMethodA("Exec", new Object[] { new JIString(command) });
					remoteExecObject
					  = (IJIDispatch)JIObjectFactory.narrowObject(
						  jvArr[0].getObjectAsComObject());

					JIVariant stdOutJIVariant = remoteExecObject.get("StdOut");
					IJIDispatch stdOut
					  = (IJIDispatch)
					    (JIObjectFactory.narrowObject(stdOutJIVariant.getObjectAsComObject()));
					JIVariant stdErrJIVariant = remoteExecObject.get("StdErr");
					IJIDispatch stdErr
					  = (IJIDispatch)
					    (JIObjectFactory.narrowObject(stdErrJIVariant.getObjectAsComObject()));
					boolean stdoutAtEndOfStream = false;
					boolean stderrAtEndOfStream = false;
					while (!stdoutAtEndOfStream || !stderrAtEndOfStream) {
						String data;

						stdoutAtEndOfStream
						  = stdoutAtEndOfStream
						    || ((JIVariant)stdOut.get("AtEndOfStream")).getObjectAsBoolean();
						if (!stdoutAtEndOfStream) {
							JIVariant jv = stdOut.callMethodA("ReadLine");
							if ((data = jv.getObjectAsString().getString()).length() != 0) {
								streamListener.stdout(data);
							}
						}
	
						stderrAtEndOfStream
						  = stderrAtEndOfStream
						    || ((JIVariant)stdErr.get("AtEndOfStream")).getObjectAsBoolean();
						if (!stderrAtEndOfStream) {
							JIVariant jv = stdErr.callMethodA("ReadLine");
							if ((data = jv.getObjectAsString().getString()).length() != 0) {
								streamListener.stderr(data);
							}
						}
					}

					exitCode
					  = ((JIVariant)remoteExecObject.get("ExitCode")).getObjectAsInt();
				} catch (Exception e) {
					threadException = e;
				} finally {
					try {
						if (remoteExecObject != null) {
							remoteExecObject.callMethodA("Terminate");
						}
					} catch (Exception e) {
				        context.getExecutionListener().log(
				          Constants.WARN_LEVEL,
				          String.format("%s (%s:'%s')", WINDOWS_NODE_EXEC,
				          				"error terminating remote exec",
				          				e.getMessage()));
					}
				}
			}
		}
    }

}
