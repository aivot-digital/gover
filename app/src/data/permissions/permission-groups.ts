import {Permission} from "./permission";

export const PermissionGroups: {
    label: string;
    permissions: Permission[];
}[] = [];

[
    {
        label: "Assets",
        permissions: [
            Permission.ASSET_CREATE,
            Permission.ASSET_READ,
            Permission.ASSET_UPDATE,
            Permission.ASSET_DELETE,
        ],
    },
    {
        label: "Objektschema",
        permissions: [
            Permission.OBJECT_SCHEMA_CREATE,
            Permission.OBJECT_SCHEMA_READ,
            Permission.OBJECT_SCHEMA_UPDATE,
            Permission.OBJECT_SCHEMA_DELETE,
        ],
    },
    {
        label: "Objekte",
        permissions: [
            Permission.OBJECT_ITEM_CREATE,
            Permission.OBJECT_ITEM_READ,
            Permission.OBJECT_ITEM_UPDATE,
            Permission.OBJECT_ITEM_DELETE,
        ],
    },
    {
        label: "Abteilungen",
        permissions: [
            Permission.DEPARTMENT_CREATE,
            Permission.DEPARTMENT_READ,
            Permission.DEPARTMENT_UPDATE,
            Permission.DEPARTMENT_DELETE,
        ],
    },
    {
        label: "Identitätsanbieter",
        permissions: [
            Permission.IDENTITY_PROVIDER_CREATE,
            Permission.IDENTITY_PROVIDER_READ,
            Permission.IDENTITY_PROVIDER_UPDATE,
            Permission.IDENTITY_PROVIDER_DELETE,
        ],
    },
    {
        label: "Zahlungsanbieter",
        permissions: [
            Permission.PAYMENT_PROVIDER_CREATE,
            Permission.PAYMENT_PROVIDER_READ,
            Permission.PAYMENT_PROVIDER_UPDATE,
            Permission.PAYMENT_PROVIDER_DELETE,
        ],
    },
    {
        label: "Vorlagen",
        permissions: [
            Permission.PRESET_CREATE,
            Permission.PRESET_READ,
            Permission.PRESET_UPDATE,
            Permission.PRESET_DELETE,
            Permission.PRESET_PUBLISH_LOCAL,
            Permission.PRESET_PUBLISH_STORE,
        ],
    },
    {
        label: "Geheimnisse",
        permissions: [
            Permission.SECRET_CREATE,
            Permission.SECRET_READ,
            Permission.SECRET_UPDATE,
            Permission.SECRET_DELETE,
        ],
    },
    {
        label: "Systemkonfiguration",
        permissions: [
            Permission.SYSTEM_CONFIG_CREATE,
            Permission.SYSTEM_CONFIG_READ,
            Permission.SYSTEM_CONFIG_UPDATE,
            Permission.SYSTEM_CONFIG_DELETE,
        ],
    },
    {
        label: "Systemrollen",
        permissions: [
            Permission.SYSTEM_ROLE_CREATE,
            Permission.SYSTEM_ROLE_READ,
            Permission.SYSTEM_ROLE_UPDATE,
            Permission.SYSTEM_ROLE_DELETE,
        ],
    },
    {
        label: "Domänenrollen",
        permissions: [
            Permission.DOMAIN_ROLE_CREATE,
            Permission.DOMAIN_ROLE_READ,
            Permission.DOMAIN_ROLE_UPDATE,
            Permission.DOMAIN_ROLE_DELETE,
        ],
    },
    {
        label: "Teams",
        permissions: [
            Permission.TEAM_CREATE,
            Permission.TEAM_READ,
            Permission.TEAM_UPDATE,
            Permission.TEAM_DELETE,
        ],
    },
    {
        label: "Themes",
        permissions: [
            Permission.THEME_CREATE,
            Permission.THEME_READ,
            Permission.THEME_UPDATE,
            Permission.THEME_DELETE,
        ],
    },
    {
        label: "Benutzerkonfiguration",
        permissions: [
            Permission.USER_CONFIG_CREATE,
            Permission.USER_CONFIG_READ,
            Permission.USER_CONFIG_UPDATE,
            Permission.USER_CONFIG_DELETE,
        ],
    },
    {
        label: "Benutzer",
        permissions: [
            Permission.USER_CREATE,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE,
        ],
    },
    {
        label: "Plugins",
        permissions: [
            Permission.PLUGIN_CREATE,
            Permission.PLUGIN_READ,
            Permission.PLUGIN_UPDATE,
            Permission.PLUGIN_DELETE,
        ],
    },
    {
        label: "Vertretungen",
        permissions: [
            Permission.DEPUTY_CREATE,
            Permission.DEPUTY_READ,
            Permission.DEPUTY_UPDATE,
            Permission.DEPUTY_DELETE,
        ],
    },
    {
        label: "Abteilungsmitgliedschaften",
        permissions: [
            Permission.DEPARTMENT_MEMBERSHIP_CREATE,
            Permission.DEPARTMENT_MEMBERSHIP_READ,
            Permission.DEPARTMENT_MEMBERSHIP_UPDATE,
            Permission.DEPARTMENT_MEMBERSHIP_DELETE,
        ],
    },
    {
        label: "Teammitgliedschaften",
        permissions: [
            Permission.TEAM_MEMBERSHIP_CREATE,
            Permission.TEAM_MEMBERSHIP_READ,
            Permission.TEAM_MEMBERSHIP_UPDATE,
            Permission.TEAM_MEMBERSHIP_DELETE,
        ],
    },
    {
        label: "Domänenressourcen-Berechtigungen",
        permissions: [
            Permission.DOMAIN_RESOURCE_PERMISSION_CREATE,
            Permission.DOMAIN_RESOURCE_PERMISSION_READ,
            Permission.DOMAIN_RESOURCE_PERMISSION_UPDATE,
            Permission.DOMAIN_RESOURCE_PERMISSION_DELETE,
        ],
    },
    {
        label: "Prozessdefinitionen",
        permissions: [
            Permission.PROCESS_DEFINITION_CREATE,
            Permission.PROCESS_DEFINITION_READ,
            Permission.PROCESS_DEFINITION_UPDATE,
            Permission.PROCESS_DEFINITION_DELETE,
            Permission.PROCESS_DEFINITION_AUDIT,
            Permission.PROCESS_DEFINITION_PUBLISH_TEST,
            Permission.PROCESS_DEFINITION_PUBLISH_LOCAL,
            Permission.PROCESS_DEFINITION_PUBLISH_STORE,
        ],
    },
    {
        label: "Prozessinstanzen",
        permissions: [
            Permission.PROCESS_INSTANCE_TRIGGER,
            Permission.PROCESS_INSTANCE_READ,
            Permission.PROCESS_INSTANCE_UPDATE,
            Permission.PROCESS_INSTANCE_DELETE,
            Permission.PROCESS_INSTANCE_PAUSE_RESUME,
            Permission.PROCESS_INSTANCE_EDIT_DATA,
            Permission.PROCESS_INSTANCE_REASSIGN,
            Permission.PROCESS_INSTANCE_COMMUNICATION_INTERNAL,
            Permission.PROCESS_INSTANCE_COMMUNICATION_EXTERNAL,
            Permission.PROCESS_INSTANCE_EDIT_TASK,
        ],
    },
];