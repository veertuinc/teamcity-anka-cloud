package com.veertu;

import com.veertu.ankaMgmtSdk.AnkaMgmtVm;
import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;


/**
 * Created by Asaf Gur.
 */

public class AnkaSSHPropertiesSetter implements AnkaPropertiesSetter{

    private final String host;
    private final int port;
    private final AnkaMgmtVm vm;
    private final String agentPath;
    private final String propetiesFilePath;
    private final String loadScriptPath;
    private String userName;
    private String password;
    private Session session;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaSSHPropertiesSetter(AnkaMgmtVm vm, String userName, String password, String agentPath) {
        this.vm = vm;
        this.host = vm.getConnectionIp();
        this.port = vm.getConnectionPort();
        this.userName = userName;
        this.password = password;
        this.agentPath = agentPath;
        this.propetiesFilePath = String.format("%s/conf/buildAgent.properties", this.agentPath);
        this.loadScriptPath = String.format("%s/bin/mac.launchd.sh", this.agentPath);
    }

    public void setProperties(Map<String, String> properties) throws AnkaUnreachableInstanceException {
        try {
            String fname= "/tmp/tc-anka.txt";
            this.sshConnect();
            String commandFmt = "echo \"%s=%s\" >> " + fname;

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String command = String.format(commandFmt, entry.getKey(), entry.getValue());
                String output = this.sendCommand(command);
                System.out.println(command);
                System.out.println(output);
            }
            this.sendCommand(this.loadScriptPath + " unload");
            this.sendCommand(this.loadScriptPath + " load");

            LOG.info("SSH properties set successfully");
        } catch (JSchException e) {
            LOG.info(String.format("ssh connect failed: %s", e.getMessage()));
            throw new AnkaUnreachableInstanceException(String.format("Instance %s is unreachable by ssh", this.vm.getId()));
        } finally {
            this.closeConnection();
        }


    }

    private void sshConnect() throws JSchException {

        JSch jsch=new JSch();

        LOG.info(String.format("Connect to %s@%s:%d", userName, host, port));
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
