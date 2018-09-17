package com.veertu.ankaMgmtSdk;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import com.veertu.AnkaCloudConnector;
import com.veertu.ankaMgmtSdk.exceptions.AnkaMgmtException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Asaf Gur on 18/05/2017.
 */

public class AnkaAPI {

    private static AnkaAPI ourInstance = new AnkaAPI();
    private Map<String, AnkaMgmtCommunicator> communicators;
    private static int vmCounter = 1;

    public static AnkaAPI getInstance() {
        return ourInstance;
    }
    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);
    private AnkaAPI() {
        this.communicators = new HashMap<String, AnkaMgmtCommunicator>();
    }

    private AnkaMgmtCommunicator getCommunicator(String mgmtURL) throws AnkaMgmtException {
        String communicatorKey = mgmtURL;
        AnkaMgmtCommunicator communicator = this.communicators.get(communicatorKey);
        if (communicator == null) {
            communicator = new AnkaMgmtCommunicator(mgmtURL);
            this.communicators.put(communicatorKey, communicator);
        }
        return communicator;
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtURL, String templateId,
                                 String tag, String nameTemplate, int sshPort, String groupId) throws AnkaMgmtException {

        LOG.info(String.format("making anka vm, url: %s, " +
                "templateId: %s, sshPort: %d", mgmtURL, templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
        String sessionId = communicator.startVm(templateId, tag, nameTemplate, groupId);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public AnkaMgmtVm getVm(String mgmtURL, String sessionId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
        AnkaVmSession ankaVmSession = communicator.showVm(sessionId);
        return new ConcAnkaMgmtVm(communicator, ankaVmSession);
    }

    public List<AnkaMgmtVm> listVms(String mgmtURL) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
        List<AnkaMgmtVm> vms = new ArrayList<>();
        List<AnkaVmSession> ankaVmSessions = communicator.list();
        for (AnkaVmSession vmSession: ankaVmSessions) {
            AnkaMgmtVm vm = new ConcAnkaMgmtVm(communicator, vmSession);
            vms.add(vm);
        }
        return vms;
    }

    public List<AnkaVmTemplate> listTemplates(String mgmtURL) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String mgmtURL, String masterVmId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
        return communicator.getTemplateTags(masterVmId);
    }

    public List<NodeGroup> getNodeGroups(String mgmtUrl) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtUrl);
        return communicator.getNodeGroups();
    }

    public AnkaCloudStatus status(String mgmtURL) {
        try {
            AnkaMgmtCommunicator communicator = getCommunicator(mgmtURL);
            return communicator.status();
        } catch (AnkaMgmtException e) {
            return null;
        }

    }
}
