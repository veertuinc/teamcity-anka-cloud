package com.veertu.common;

public interface AnkaConstants {

    // Cloud Profile constants
    String PROFILE_SETTING_HTML = "profile-settings.html";
    String PROFILE_SETTINGS_JSP = "profile-settings.jsp";
    String INSTANCE_ID = "instance_id";
    String IMAGE_ID = "image_id";
    String TEMPLATE_NAME = "TEMPLATE_NAME";
    String TEMPLATE_TAG = "TEMPLATE_TAG";
    String VM_NAME_TEMPLATE = "VM_NAME_TEMPLATE";
    String GROUP_ID = "group_id";
    String CONTROLLER_URL_NAME = "clouds.anka.url";
    String CLOUD_CODE = "anka";
    String CLOUD_DISPLAY_NAME = "Anka Build Cloud";
    String SSH_USER = "ssh_user";
    String SSH_PASSWORD = "ssh_password";
    String SSH_FORWARDING_PORT = "sshForwardingPort";
    String AGENT_PATH = "agent_path";
    String OPTIONAL_SERVER_URL = "profileServerUrl";
    String SKIP_TLS_VERIFICATION = "skipTLSVerification";
    String AGENT_POOL_ID = "agentPoolId";
    String PRIORITY = "priority";
    String MAX_INSTANCES = "clouds.anka.maxInstances";
    String AUTH_METHOD = "auth.method";
    String AUTH_METHOD_CERT = "cert";
    String AUTH_METHID_OIDC = "oidc";
    String OIDC_CLIENT_ID = "auth.oidc.client_id";
    String OIDC_CLIENT_SECRET = "auth.oidc.client_secret";
    String CERT_STRING = "auth.cert.cert";
    String CERT_KEY_STRING = "auth.cert.cert_key";
    String ROOT_CA = "auth.cert.rootca";
    String PROP_PREFIX = "prop:";

    // Agent Configuration constants
    String ENV_INSTANCE_ID_KEY = "env.ANKA_INSTANCE_ID";
    String ENV_TEMPLATE_ID_KEY = "env.ANKA_TEMPLATE_ID";
    String ENV_SERVER_URL_KEY = "serverUrl";
    String ENV_ANKA_CLOUD_KEY = "env.ANKA_CLOUD";
    String ENV_ANKA_CLOUD_VALUE = "ANKA_BUILD_CLOUD";
    String ENV_PROFILE_ID = "env.ANKA_PROFILE_ID";
    String ENV_AGENT_NAME_KEY = "name";
}
