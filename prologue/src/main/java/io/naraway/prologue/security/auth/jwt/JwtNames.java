package io.naraway.prologue.security.auth.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtNames {
    //
    public static final String PARAMETER_DISPLAY_NAME = "display_name";
    public static final String PARAMETER_EMAIL = "email";
    public static final String PARAMETER_ATTRIBUTES = "attributes";

    public static final String ATTRIBUTES_LOCATION = "location";
    public static final String ATTRIBUTES_DEVICE_IP = "device_ip";
    public static final String ATTRIBUTES_OSID = "osid";
    public static final String ATTRIBUTES_USID = "usid";
    public static final String ATTRIBUTES_CITIZEN_ID = "citizen_id";
    public static final String ATTRIBUTES_PAVILION_ID = "pavilion_id";
    public static final String ATTRIBUTES_CINEROOM_IDS = "cineroom_ids";
    public static final String ATTRIBUTES_CITIZEN_USER_ID = "citizen_user_id";
    public static final String ATTRIBUTES_CITIZEN_SESSION_ID = "citizen_session_id";
    public static final String ATTRIBUTES_SERVICE_USER_ID = "service_user_id";
    public static final String ATTRIBUTES_SERVICE_ROLES = "service_roles";
}