package com.spcow.winrmclient;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.CommandInterpreter;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;

public class SendFileWinRMOperation extends WinRMOperation implements Serializable {

    private final String source;
    private final String destination;
    private final String configurationName;
    private final String SEND_FILE_PATH = "/com/spcow/winrmclient/SendFileWinRMOperation/Send-File.ps1";

    @DataBoundConstructor
    public SendFileWinRMOperation(String source, String destination, String configurationName) {
        this.source = source;
        this.destination = destination;
        this.configurationName = configurationName;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public boolean runOperation(Run<?, ?> run, FilePath buildWorkspace, Launcher launcher, TaskListener listener,
                                String hostName, String userName, String password) {
        boolean result = false;
        try {
            StreamSource ssSendFileCommand = new StreamSource(WinRMClientBuilder.class.getResourceAsStream(SEND_FILE_PATH));
            final String strRemoteSendFile = Utils.getStringFromInputStream(ssSendFileCommand.getInputStream());
            CommandInterpreter ciSendFile = new CommandInterpreter(strRemoteSendFile) {
                @Override
                public String[] buildCommandLine(FilePath filePath) {
                    return new String[0];
                }

                @Override
                protected String getContents() {
                    return strRemoteSendFile;
                }

                @Override
                protected String getFileExtension() {
                    return Utils.getFileExtension();
                }
            };
            FilePath fpRemoteSendFile = ciSendFile.createScriptFile(buildWorkspace);
            StringBuilder sb = new StringBuilder();
            sb.append(". " + fpRemoteSendFile.getRemote());
            sb.append(System.lineSeparator());
            sb.append("Send-File");
            sb.append(" ");
            sb.append("\"" + source + "\"");
            sb.append(" ");
            sb.append("\"" + destination + "\"");
            sb.append(" ");
            sb.append("\"" + hostName + "\"");
            sb.append(" ");
            sb.append("\"" + userName + "\"");
            sb.append(" ");
            sb.append("\"" + password + "\"");
            if (configurationName != null) {
                sb.append(" ");
                sb.append("\"" + configurationName + "\"");
            }
            CommandInterpreter remoteCommandInterpreter = new CommandInterpreter(sb.toString()) {
                @Override
                public String[] buildCommandLine(FilePath script) {
                    return Utils.buildCommandLine(script);
                }

                @Override
                protected String getContents() {
                    return Utils.getContents(command);
                }

                @Override
                protected String getFileExtension() {
                    return Utils.getFileExtension();
                }

            };
            FilePath scriptFile = remoteCommandInterpreter.createScriptFile(buildWorkspace);
            remoteCommandInterpreter.buildCommandLine(scriptFile);
            int exitStatus = launcher.launch().cmds(remoteCommandInterpreter.buildCommandLine(scriptFile)).stdout(listener).join();
            scriptFile.delete();
            fpRemoteSendFile.delete();
            result = didErrorsOccur(exitStatus);

        } catch (Exception e) {
            listener.fatalError(e.getMessage());
        }
        return result;
    }

    private boolean didErrorsOccur(int exitStatus) {
        boolean result = true;
        if (exitStatus != 0) {
            result = false;
        }
        return result;
    }

    @Extension
    public static class DescriptorImpl extends WinRMOperationDescriptor {
        public String getDisplayName() {
            return "Send-File";
        }

    }
}
