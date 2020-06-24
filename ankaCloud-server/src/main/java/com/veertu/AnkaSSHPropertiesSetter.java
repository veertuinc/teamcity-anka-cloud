package com.veertu;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import com.intellij.openapi.diagnostic.Logger;
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
    private final String propetiesFilePath;
    private final String loadScriptPath;
    private String userName;
    private String password;
    private Session session;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);
    private final int SSH_PORT = 22;  // TODO: add option for user to specify this per cloud profile

    public AnkaSSHPropertiesSetter(AnkaVmInstance vm, String userName, String password, String agentPath) {
        this.vm = vm;
        this.host = vm.getVmInfo().getHostIp();
        this.port = vm.getVmInfo().getForwardedPort(SSH_PORT);
        this.userName = userName;
        this.password = password;
        this.agentPath = agentPath;
        this.propetiesFilePath = String.format("%s/conf/buildAgent.properties", this.agentPath);
        this.loadScriptPath = String.format("%s/bin/mac.launchd.sh", this.agentPath);
    }

    public void setProperties(Map<String, String> properties) throws AnkaUnreachableInstanceException {
        try {
            this.sshConnect();
            String commandFmt = "echo \"%s=%s\" >> " + this.propetiesFilePath;

            //this.sendCommand("cat /dev/null > " + this.propetiesFilePath);
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
           throw new AnkaUnreachableInstanceException(String.format("Instance %s is unreachable by ssh", this.vm.getId()));
        } finally {
            this.closeConnection();
        }


    }

    private void sshConnect() throws JSchException {

        JSch jsch=new JSch();

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
