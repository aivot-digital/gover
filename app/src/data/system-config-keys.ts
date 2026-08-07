export const SystemConfigKeys = {
    provider: {
        name: 'ProviderName',
        listingPage: {
            imprintDepartmentId: 'ProviderListingPageImprintDepartmentId',
            privacyDepartmentId: 'ProviderListingPagePrivacyDepartmentId',
            accessibilityDepartmentId: 'ProviderListingPageAccessibilityDepartmentId',
            customListingPageLink: 'ProviderListingPageCustomLink',
            disableListingPageLink: 'ProviderListingPageDisableLink',
            disableProsunaListingPage: 'ProviderListingPageDisablePublicListingPage',
        },
    },
    system: {
        theme: 'SystemTheme',
        logo: 'SystemLogo',
        favicon: 'SystemFavicon',
    },
    prosuna: {
        storeKey: 'ProsunaStoreKey',
    },
    users: {
        defaultSystemRole: 'users.default_system_role',
    },
    systemRoles: {
        mostPrivilegedRole: 'system_roles.most_privileged_role',
    },
    storage: {
        assets: {
            default_storage_provider: 'storage.assets.default_storage_provider',
        },
        attachments: {
            default_storage_provider: 'storage.attachments.default_storage_provider',
        },
    },
};
