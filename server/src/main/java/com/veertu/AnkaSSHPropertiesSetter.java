package com.veertu;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.veertu.ankaMgmtSdk.AnkaVmInstance;

import jetbrains.buildServer.log.Loggers;


/**
 * Created by Asaf Gur.
 */

public class AnkaSSHPropertiesSetter implements AnkaPropertiesSetter{

    private final String host;
    private final int port;
    private final AnkaVmInstance vm;
    private final String agentPath;
    private final String propertiesFilePath;
    private final String loadScriptPath;
    private String userName;
    private String password;
    private int sshForwardingPort;
    private String serverUrl;
    private Session session;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaSSHPropertiesSetter(AnkaVmInstance vm, String userName, String password, String agentPath, int sshForwardingPort, String serverUrl) {
        this.vm = vm;
        this.host = vm.getVmInfo().getHostIp();
        this.sshForwardingPort = sshForwardingPort;
        this.port = vm.getVmInfo().getForwardedPort(this.sshForwardingPort);
        this.userName = userName;
        this.password = password;
        this.serverUrl = serverUrl;
        this.agentPath = agentPath;
        this.propertiesFilePath = String.format("%s/conf/buildAgent.properties", this.agentPath);
        this.loadScriptPath = String.format("%s/bin/mac.launchd.sh", this.agentPath);
    }

    public void setProperties(Map<String, String> properties) throws AnkaUnreachableInstanceException {
        try {
            this.sshConnect();

            // install the buildAgent from teamcity itself
            String installAgentCommand = String.format("if [ ! -d \"%s\" ]; then mkdir -p %s && cd %s && curl -O -L %s/update/buildAgentFull.zip && unzip buildAgentFull.zip; fi", this.agentPath, this.agentPath, this.agentPath, this.serverUrl);
            String installAgentOutput = this.sendCommand(installAgentCommand);
            LOG.info(String.format("ssh command: %s", installAgentCommand));
            LOG.info(String.format("ssh command output: %s", installAgentOutput));

            // clear the buildAgent.properties so existing example lines don't screw up registration
            String clearbuildAgentPropertiesCommand = "cat /dev/null > " + this.propertiesFilePath;
            this.sendCommand(clearbuildAgentPropertiesCommand);

            // add everything we need to the buildAgent.properties
            String commandFmt = "echo \"%s=%s\" >> " + this.propertiesFilePath;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String command = String.format(commandFmt, entry.getKey(), entry.getValue());
                String output = this.sendCommand(command);
                LOG.info(String.format("ssh command: %s", command));
                LOG.info(String.format("ssh command output: %s", output));
            }

            // launch
            this.sendCommand(this.loadScriptPath + " unload");
            this.sendCommand(this.loadScriptPath + " load");

            LOG.info(String.format("successful ssh into vm %s", this.vm.getId()));
        } catch (JSchException e) {
           throw new AnkaUnreachableInstanceException(String.format("Instance %s is unreachable by ssh: %s", this.vm.getId(), e.getMessage()));
        } finally {
            this.closeConnection();
        }


    }

    private void sshConnect() throws JSchException {

        JSch jsch=new JSch();

        LOG.info(String.format("attempting SSH connection to Instance %s with Username %s, Host %s, and Port %s", vm.getId(), userName, host, port));

        Session session = jsch.getSession(userName, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect(30000);
        this.session = session;

    }

    private void closeConnection() {
        if (this.session != null && this.session.isConnected()) {
            this.session.disconnect();
        }
    }

    private String sendCommand(String command)
    {
        StringBuilder outputBuffer = new StringBuilder();

        try
        {
            Channel channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();

            while(readByte != 0xffffffff)
            {
                outputBuffer.append((char)readByte);
                readByte = commandOutput.read();
            }

            channel.disconnect();
        }
        catch(IOException ioX)
        {
            ioX.printStackTrace();
            return null;
        }
        catch(JSchException jschX)
        {
            jschX.printStackTrace();
            return null;
        }

        return outputBuffer.toString();
    }
}
