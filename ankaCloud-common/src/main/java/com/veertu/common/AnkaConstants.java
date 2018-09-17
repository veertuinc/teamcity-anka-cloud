package com.veertu.common;

public interface AnkaConstants {

    // Cloud Profile constants
    String PROFILE_SETTING_HTML = "profile-settings.html";
    String PROFILE_SETTINGS_JSP = "profile-settings.jsp";
    String INSTANCE_ID = "instance_id";
    String IMAGE_ID = "image_id";
    String IMAGE_NAME = "image_name";
    String IMAGE_TAG = "image_tag";
    String GROUP_ID = "group_id";
    String CONTROLLER_URL_NAME = "clouds.anka.url";
    String CLOUD_CODE = "anka";
    String CLOUD_DISPLAY_NAME = "Anka Build Cloud";
    String SSH_USER = "ssh_user";
    String SSH_PASSWORD = "ssh_password";
    String AGENT_PATH = "agent_path";
    String OPTIONAL_SERVER_URL = "profileServerUrl";
    String AGENT_POOL_ID = "agentPoolId";
    String MAX_INSTANCES = "clouds.anka.maxInstances";

    // Agent Configuration constants
    String ENV_INSTANCE_ID_KEY = "env.INSTANCE_ID";
    String ENV_IMAGE_ID_KEY = "env.IMAGE_ID";
    String ENV_SERVER_URL_KEY = "serverUrl";
    String ENV_ANKA_CLOUD_KEY = "env.ANKA_CLOUD";
    String ENV_ANKA_CLOUD_VALUE = "ANKA";
    String ENV_PROFILE_ID = "env.ANKA_PROFILE_ID";
    String ENV_AGENT_NAME_KEY = "name";
}
