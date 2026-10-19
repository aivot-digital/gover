package de.aivot.prosuna.backend.seeding.seeds;

import de.aivot.prosuna.backend.asset.permissions.AssetPermissionProvider;
import de.aivot.prosuna.backend.audit.permissions.AuditPermissionProvider;
import de.aivot.prosuna.backend.codeLists.permissions.CodeListPermissionProvider;
import de.aivot.prosuna.backend.communication.permissions.CommunicationProviderPermissionProvider;
import de.aivot.prosuna.backend.config.entities.SystemConfigEntity;
import de.aivot.prosuna.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.identity.permissions.IdentityProviderPermissionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.DomainResourcePermissionProvider;
import de.aivot.prosuna.backend.permissions.permissions.PermissionSetPermissionProvider;
import de.aivot.prosuna.backend.plugin.permissions.PluginPermissionProvider;
import de.aivot.prosuna.backend.preset.permissions.PresetPermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.secrets.permissions.SecretPermissionProvider;
import de.aivot.prosuna.backend.seeding.models.SeedProvider;
import de.aivot.prosuna.backend.storage.permissions.StoragePermissionProvider;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.theme.permissions.ThemePermissionProvider;
import de.aivot.prosuna.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.userRoles.configs.MostPrivilegedSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.prosuna.backend.userRoles.permissions.DomainRolePermissionProvider;
import de.aivot.prosuna.backend.userRoles.permissions.SystemRolePermissionProvider;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import de.aivot.prosuna.backend.userRoles.services.SystemRoleService;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemRolesSeed implements SeedProvider {
    private final SystemRoleService systemRoleService;
    private final SystemRoleRepository systemRoleRepository;
    private final SystemConfigService systemConfigService;

    public SystemRolesSeed(SystemRoleService systemRoleService,
                           SystemRoleRepository systemRoleRepository,
                           SystemConfigService systemConfigService) {
        this.systemRoleService = systemRoleService;
        this.systemRoleRepository = systemRoleRepository;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public boolean shouldSeed() {
        // System roles are editable defaults, so they are only created for an installation without any system role.
        return systemRoleRepository.count() == 0;
    }

    @Override
    public void seed() throws ResponseException {
        var superAdministrator = systemRoleService.create(new SystemRoleEntity()
                .setName("Superadministrator:in")
                .setDescription("Hat uneingeschränkten Zugriff auf alle Funktionen und Einstellungen des Systems.")
                .setPermissions(List.of(
                        AssetPermissionProvider.ASSET_CREATE,
                        AssetPermissionProvider.ASSET_READ,
                        AssetPermissionProvider.ASSET_UPDATE,
                        AssetPermissionProvider.ASSET_DELETE,

                        CodeListPermissionProvider.CODE_LIST_CREATE,
                        CodeListPermissionProvider.CODE_LIST_READ,
                        CodeListPermissionProvider.CODE_LIST_UPDATE,
                        CodeListPermissionProvider.CODE_LIST_DELETE,
                        CodeListPermissionProvider.CODE_LIST_EXPORT,

                        DataObjectPermissionProvider.OBJECT_SCHEMA_CREATE,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_READ,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_DELETE,

                        DataObjectPermissionProvider.OBJECT_ITEM_CREATE,
                        DataObjectPermissionProvider.OBJECT_ITEM_READ,
                        DataObjectPermissionProvider.OBJECT_ITEM_UPDATE,
                        DataObjectPermissionProvider.OBJECT_ITEM_DELETE,

                        DepartmentPermissionProvider.DEPARTMENT_CREATE,
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_UPDATE,
                        DepartmentPermissionProvider.DEPARTMENT_DELETE,

                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_DELETE,

                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_DELETE,

                        PresetPermissionProvider.PRESET_CREATE,
                        PresetPermissionProvider.PRESET_READ,
                        PresetPermissionProvider.PRESET_UPDATE,
                        PresetPermissionProvider.PRESET_DELETE,
                        PresetPermissionProvider.PRESET_PUBLISH_LOCAL,
                        PresetPermissionProvider.PRESET_PUBLISH_MARKETPLACE,

                        SecretPermissionProvider.SECRET_CREATE,
                        SecretPermissionProvider.SECRET_READ,
                        SecretPermissionProvider.SECRET_UPDATE,
                        SecretPermissionProvider.SECRET_DELETE,

                        ConfigPermissionProvider.SYSTEM_CONFIG_CREATE,
                        ConfigPermissionProvider.SYSTEM_CONFIG_READ,
                        ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE,
                        ConfigPermissionProvider.SYSTEM_CONFIG_DELETE,

                        SystemRolePermissionProvider.SYSTEM_ROLE_CREATE,
                        SystemRolePermissionProvider.SYSTEM_ROLE_READ,
                        SystemRolePermissionProvider.SYSTEM_ROLE_UPDATE,
                        SystemRolePermissionProvider.SYSTEM_ROLE_DELETE,

                        DomainRolePermissionProvider.DOMAIN_ROLE_CREATE,
                        DomainRolePermissionProvider.DOMAIN_ROLE_READ,
                        DomainRolePermissionProvider.DOMAIN_ROLE_UPDATE,
                        DomainRolePermissionProvider.DOMAIN_ROLE_DELETE,

                        TeamPermissionProvider.TEAM_CREATE,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_UPDATE,
                        TeamPermissionProvider.TEAM_DELETE,

                        ThemePermissionProvider.THEME_CREATE,
                        ThemePermissionProvider.THEME_READ,
                        ThemePermissionProvider.THEME_UPDATE,
                        ThemePermissionProvider.THEME_DELETE,

                        ConfigPermissionProvider.USER_CONFIG_CREATE,
                        ConfigPermissionProvider.USER_CONFIG_READ,
                        ConfigPermissionProvider.USER_CONFIG_UPDATE,
                        ConfigPermissionProvider.USER_CONFIG_DELETE,

                        UserPermissionProvider.USER_CREATE,
                        UserPermissionProvider.USER_READ,
                        UserPermissionProvider.USER_UPDATE,
                        UserPermissionProvider.USER_DELETE,

                        PluginPermissionProvider.PLUGIN_READ,

                        UserPermissionProvider.DEPUTY_CREATE,
                        UserPermissionProvider.DEPUTY_READ,
                        UserPermissionProvider.DEPUTY_UPDATE,
                        UserPermissionProvider.DEPUTY_DELETE,

                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_CREATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_UPDATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_DELETE,

                        TeamPermissionProvider.TEAM_MEMBERSHIP_CREATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_DELETE,

                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_CREATE,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_UPDATE,
                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_DELETE,

                        PermissionSetPermissionProvider.PERMISSION_SET_READ,

                        ProcessPermissionProvider.PROCESS_DEFINITION_CREATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_READ,
                        ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_DELETE,
                        ProcessPermissionProvider.PROCESS_DEFINITION_AUDIT,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_TEST,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_LOCAL,
                        ProcessPermissionProvider.PROCESS_DEFINITION_PUBLISH_MARKETPLACE,

                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_TRIGGER,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_DELETE,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_PAUSE_RESUME,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_INTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_COMMUNICATION_EXTERNAL,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_DATA,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_RESTART_FAILED,
                        ProcessInstancePermissionProvider.PROCESS_INSTANCE_MIGRATE,

                        StoragePermissionProvider.STORAGE_PROVIDER_READ,
                        StoragePermissionProvider.STORAGE_PROVIDER_UPDATE,
                        StoragePermissionProvider.STORAGE_PROVIDER_CREATE,
                        StoragePermissionProvider.STORAGE_PROVIDER_DELETE,

                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_CREATE,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_DELETE,

                        AuditPermissionProvider.AUDIT_LOG_READ
                )));

        systemRoleService.create(new SystemRoleEntity()
                .setName("Systemadministrator:in")
                .setDescription("Hat uneingeschränkten Zugriff auf alle Systemressourcen und Einstellungen.")
                .setPermissions(List.of(
                        AssetPermissionProvider.ASSET_CREATE,
                        AssetPermissionProvider.ASSET_READ,
                        AssetPermissionProvider.ASSET_UPDATE,
                        AssetPermissionProvider.ASSET_DELETE,

                        CodeListPermissionProvider.CODE_LIST_CREATE,
                        CodeListPermissionProvider.CODE_LIST_READ,
                        CodeListPermissionProvider.CODE_LIST_UPDATE,
                        CodeListPermissionProvider.CODE_LIST_DELETE,

                        DataObjectPermissionProvider.OBJECT_SCHEMA_CREATE,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_READ,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_UPDATE,
                        DataObjectPermissionProvider.OBJECT_SCHEMA_DELETE,

                        DataObjectPermissionProvider.OBJECT_ITEM_CREATE,
                        DataObjectPermissionProvider.OBJECT_ITEM_READ,
                        DataObjectPermissionProvider.OBJECT_ITEM_UPDATE,
                        DataObjectPermissionProvider.OBJECT_ITEM_DELETE,

                        DepartmentPermissionProvider.DEPARTMENT_CREATE,
                        DepartmentPermissionProvider.DEPARTMENT_READ,
                        DepartmentPermissionProvider.DEPARTMENT_UPDATE,
                        DepartmentPermissionProvider.DEPARTMENT_DELETE,

                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE,
                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_DELETE,

                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_CREATE,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_UPDATE,
                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_DELETE,

                        PresetPermissionProvider.PRESET_CREATE,
                        PresetPermissionProvider.PRESET_READ,
                        PresetPermissionProvider.PRESET_UPDATE,
                        PresetPermissionProvider.PRESET_DELETE,
                        PresetPermissionProvider.PRESET_PUBLISH_LOCAL,
                        PresetPermissionProvider.PRESET_PUBLISH_MARKETPLACE,

                        SecretPermissionProvider.SECRET_CREATE,
                        SecretPermissionProvider.SECRET_READ,
                        SecretPermissionProvider.SECRET_UPDATE,
                        SecretPermissionProvider.SECRET_DELETE,

                        ConfigPermissionProvider.SYSTEM_CONFIG_CREATE,
                        ConfigPermissionProvider.SYSTEM_CONFIG_READ,
                        ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE,
                        ConfigPermissionProvider.SYSTEM_CONFIG_DELETE,

                        SystemRolePermissionProvider.SYSTEM_ROLE_CREATE,
                        SystemRolePermissionProvider.SYSTEM_ROLE_READ,
                        SystemRolePermissionProvider.SYSTEM_ROLE_UPDATE,
                        SystemRolePermissionProvider.SYSTEM_ROLE_DELETE,

                        DomainRolePermissionProvider.DOMAIN_ROLE_CREATE,
                        DomainRolePermissionProvider.DOMAIN_ROLE_READ,
                        DomainRolePermissionProvider.DOMAIN_ROLE_UPDATE,
                        DomainRolePermissionProvider.DOMAIN_ROLE_DELETE,

                        TeamPermissionProvider.TEAM_CREATE,
                        TeamPermissionProvider.TEAM_READ,
                        TeamPermissionProvider.TEAM_UPDATE,
                        TeamPermissionProvider.TEAM_DELETE,

                        ThemePermissionProvider.THEME_CREATE,
                        ThemePermissionProvider.THEME_READ,
                        ThemePermissionProvider.THEME_UPDATE,
                        ThemePermissionProvider.THEME_DELETE,

                        ConfigPermissionProvider.USER_CONFIG_CREATE,
                        ConfigPermissionProvider.USER_CONFIG_READ,
                        ConfigPermissionProvider.USER_CONFIG_UPDATE,
                        ConfigPermissionProvider.USER_CONFIG_DELETE,

                        UserPermissionProvider.USER_CREATE,
                        UserPermissionProvider.USER_READ,
                        UserPermissionProvider.USER_UPDATE,
                        UserPermissionProvider.USER_DELETE,

                        PluginPermissionProvider.PLUGIN_READ,

                        UserPermissionProvider.DEPUTY_CREATE,
                        UserPermissionProvider.DEPUTY_READ,
                        UserPermissionProvider.DEPUTY_UPDATE,
                        UserPermissionProvider.DEPUTY_DELETE,

                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_CREATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_UPDATE,
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_DELETE,

                        TeamPermissionProvider.TEAM_MEMBERSHIP_CREATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE,
                        TeamPermissionProvider.TEAM_MEMBERSHIP_DELETE,

                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        StoragePermissionProvider.STORAGE_PROVIDER_READ,
                        StoragePermissionProvider.STORAGE_PROVIDER_UPDATE,
                        StoragePermissionProvider.STORAGE_PROVIDER_CREATE,
                        StoragePermissionProvider.STORAGE_PROVIDER_DELETE,

                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_CREATE,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_UPDATE,
                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_DELETE
                )));

        var staff = systemRoleService.create(new SystemRoleEntity()
                .setName("Mitarbeiter:in")
                .setDescription("Hat nur Zugriff auf grundlegende Funktionen und Inhalte des Systems.")
                .setPermissions(List.of(
                        AssetPermissionProvider.ASSET_CREATE,
                        AssetPermissionProvider.ASSET_READ,
                        AssetPermissionProvider.ASSET_UPDATE,

                        CodeListPermissionProvider.CODE_LIST_READ,

                        DataObjectPermissionProvider.OBJECT_SCHEMA_READ,

                        DataObjectPermissionProvider.OBJECT_ITEM_CREATE,
                        DataObjectPermissionProvider.OBJECT_ITEM_READ,
                        DataObjectPermissionProvider.OBJECT_ITEM_UPDATE,

                        DepartmentPermissionProvider.DEPARTMENT_READ,

                        IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ,

                        PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ,

                        PresetPermissionProvider.PRESET_CREATE,
                        PresetPermissionProvider.PRESET_READ,
                        PresetPermissionProvider.PRESET_UPDATE,
                        PresetPermissionProvider.PRESET_PUBLISH_LOCAL,

                        SecretPermissionProvider.SECRET_READ,

                        ConfigPermissionProvider.SYSTEM_CONFIG_READ,

                        SystemRolePermissionProvider.SYSTEM_ROLE_READ,

                        DomainRolePermissionProvider.DOMAIN_ROLE_READ,

                        TeamPermissionProvider.TEAM_READ,

                        ThemePermissionProvider.THEME_READ,

                        UserPermissionProvider.USER_READ,

                        PluginPermissionProvider.PLUGIN_READ,

                        UserPermissionProvider.DEPUTY_READ,

                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ,

                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ,

                        DomainResourcePermissionProvider.DOMAIN_RESOURCE_PERMISSION_READ,

                        StoragePermissionProvider.STORAGE_PROVIDER_READ,

                        CommunicationProviderPermissionProvider.COMMUNICATION_PROVIDER_READ
                )));

        // Reference the created roles instead of relying on the configs' default IDs, which only match a fresh sequence.
        saveSystemConfig(MostPrivilegedSystemRoleSystemConfigDefinition.KEY, superAdministrator.getId());
        saveSystemConfig(DefaultUserSystemRoleSystemConfigDefinition.KEY, staff.getId());
    }

    private void saveSystemConfig(@Nonnull String key, @Nonnull Integer systemRoleId) throws ResponseException {
        systemConfigService.save(key, new SystemConfigEntity().setValue(String.valueOf(systemRoleId)));
    }
}
