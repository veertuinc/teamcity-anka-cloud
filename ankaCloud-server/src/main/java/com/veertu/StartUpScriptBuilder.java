package com.veertu;

import com.veertu.common.AnkaConstants;

import java.util.Map;

public class StartUpScriptBuilder {


    private final String serverUrl;
    private final String instanceId;
    private final String agentPath;
    private final String propetiesFilePath;
    private final String loadScriptPath;
    private final String ankaPropertiesFilePath;

    public StartUpScriptBuilder(String serverUrl, String instanceId, String agentPath ) {
        this.serverUrl = serverUrl;
        this.instanceId = instanceId;
        this.agentPath = agentPath;
        this.propetiesFilePath = String.format("%s/conf/buildAgent.properties", this.agentPath);
        this.loadScriptPath = String.format("%s/bin/mac.launchd.sh", this.agentPath);
        this.ankaPropertiesFilePath = "/tmp/tc-anka.txt";
    }

    public String makeStartupScript(Map<String, String> properties) {

        StringBuilder script = new StringBuilder();
        if (serverUrl != null && !serverUrl.isEmpty()) {
            script.append(String.format("echo \"%s=%s\" >> %s \n", AnkaConstants.ENV_SERVER_URL_KEY, serverUrl, this.propetiesFilePath));
        }
        script.append(String.format("echo \"%s=%s\" > %s\n", AnkaConstants.INSTANCE_ID, instanceId, ankaPropertiesFilePath));

        String commandFmt = "echo \"%s=%s\" >> " + ankaPropertiesFilePath + "\n";

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String command = String.format(commandFmt, entry.getKey(), entry.getValue());
            script.append(command);
            System.out.println(command);
        }

        script.append(this.loadScriptPath).append(" unload \n");
        script.append(this.loadScriptPath).append(" load \n");


        return script.toString();
    }

}
