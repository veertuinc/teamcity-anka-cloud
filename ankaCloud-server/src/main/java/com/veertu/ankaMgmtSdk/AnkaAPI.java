package com.veertu.ankaMgmtSdk;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Asaf Gur on 18/05/2017.
 */

public class AnkaAPI {

    private final String mgmtURL;
    private AnkaMgmtCommunicator communicator;
    private static int vmCounter = 1;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    public AnkaAPI(String mgmtUrl) {
        this.mgmtURL = mgmtUrl;
        this.communicator = new AnkaMgmtCommunicator(mgmtUrl);
    }

    public AnkaAPI(String mgmtUrl, boolean skipTLSVerification, String client, String key, AuthType authType) {
        this.mgmtURL = mgmtUrl;

        switch (authType) {
            case CERTIFICATE:
                this.communicator = new AnkaMgmtClientCertAuthCommunicator(mgmtUrl, skipTLSVerification, client, key);
                break;
            case OPENID_CONNECT:
                this.communicator = new AnkaMgmtOpenIdCommunicator(mgmtUrl, skipTLSVerification, client, key);
                break;
        }
    }

    public AnkaAPI(String mgmtURL, boolean skipTLSVerification) {
        this.mgmtURL = mgmtURL;
        this.communicator = new AnkaMgmtCommunicator(mgmtURL, skipTLSVerification);
    }


    public AnkaMgmtVm makeAnkaVm(String templateId,
                                 String tag, String nameTemplate, int sshPort, int priority, String groupId) throws AnkaMgmtException {

        LOG.info(String.format("making anka vm, url: %s, " +
                "templateId: %s, sshPort: %d", mgmtURL, templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        String sessionId = communicator.startVm(templateId, tag, nameTemplate, null, groupId, priority);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public AnkaMgmtVm getVm(String sessionId) throws AnkaMgmtException {
        AnkaVmSession ankaVmSession = communicator.showVm(sessionId);
        return new ConcAnkaMgmtVm(communicator, ankaVmSession);
    }

    public List<AnkaMgmtVm> listVms() throws AnkaMgmtException {
        List<AnkaMgmtVm> vms = new ArrayList<>();
        List<AnkaVmSession> ankaVmSessions = communicator.list();
        for (AnkaVmSession vmSession: ankaVmSessions) {
            AnkaMgmtVm vm = new ConcAnkaMgmtVm(communicator, vmSession);
            vms.add(vm);
        }
        return vms;
    }

    public List<AnkaVmTemplate> listTemplates() throws AnkaMgmtException {
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String masterVmId) throws AnkaMgmtException {
        return communicator.getTemplateTags(masterVmId);
    }

    public List<NodeGroup> getNodeGroups() throws AnkaMgmtException {
        return communicator.getNodeGroups();
    }

    public AnkaCloudStatus status() {
        try {
            return communicator.status();
        } catch (AnkaMgmtException e) {
            return null;
        }

    }

}
