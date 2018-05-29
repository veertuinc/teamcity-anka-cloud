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

    private AnkaMgmtCommunicator getCommunicator(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        String communicatorKey = mgmtHost + ":" + mgmtPort;
        AnkaMgmtCommunicator communicator = this.communicators.get(communicatorKey);
        if (communicator == null) {
            communicator = new AnkaMgmtCommunicator(mgmtHost, mgmtPort);
            this.communicators.put(communicatorKey, communicator);
        }
        return communicator;
    }

    public AnkaMgmtVm makeAnkaVm(String mgmtHost, String mgmtPort, String templateId,
                                 String tag, String nameTemplate, int sshPort) throws AnkaMgmtException {

        LOG.info(String.format("making anka vm, host: %s, port: %s, " +
                "templateId: %s, sshPort: %d", mgmtHost, mgmtPort, templateId, sshPort));
        if (nameTemplate == null || nameTemplate.isEmpty())
            nameTemplate = "$template_name-$node_name-$ts";
        else if (!nameTemplate.contains("$ts"))
            nameTemplate = String.format("%s-%d", nameTemplate, vmCounter++);

        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        String sessionId = communicator.startVm(templateId, tag, nameTemplate);
        AnkaMgmtVm vm = new ConcAnkaMgmtVm(sessionId, communicator, sshPort);
        return vm;

    }

    public AnkaMgmtVm getVm(String mgmtHost, String mgmtPort, String sessionId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        AnkaVmSession ankaVmSession = communicator.showVm(sessionId);
        return new ConcAnkaMgmtVm(communicator, ankaVmSession);
    }

    public List<AnkaMgmtVm> listVms(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        List<AnkaMgmtVm> vms = new ArrayList<>();
        List<AnkaVmSession> ankaVmSessions = communicator.list();
        for (AnkaVmSession vmSession: ankaVmSessions) {
            AnkaMgmtVm vm = new ConcAnkaMgmtVm(communicator, vmSession);
            vms.add(vm);
        }
        return vms;
    }

    public List<AnkaVmTemplate> listTemplates(String mgmtHost, String mgmtPort) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, mgmtPort);
        return communicator.listTemplates();
    }

    public List<String> listTemplateTags(String mgmtHost, String ankaMgmtPort, String masterVmId) throws AnkaMgmtException {
        AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, ankaMgmtPort);
        return communicator.getTemplateTags(masterVmId);
    }

    public AnkaCloudStatus status(String mgmtHost, String ankaMgmtPort) {
        try {
            AnkaMgmtCommunicator communicator = getCommunicator(mgmtHost, ankaMgmtPort);
            return communicator.status();
        } catch (AnkaMgmtException e) {
            return null;
        }

    }
}
