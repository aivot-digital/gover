export const SystemConfigKeys = {
    provider: {
        name: 'ProviderName',
        listingPage: {
            imprintDepartmentId: 'ProviderListingPageImprintDepartmentId',
            privacyDepartmentId: 'ProviderListingPagePrivacyDepartmentId',
            accessibilityDepartmentId: 'ProviderListingPageAccessibilityDepartmentId',
            customListingPageLink: 'ProviderListingPageCustomLink',
            disableListingPageLink: 'ProviderListingPageDisableLink',
            disableGoverListingPage: 'ProviderListingPageDisablePublicListingPage',
        },
    },
    system: {
        theme: 'SystemTheme',
        logo: 'SystemLogo',
        favicon: 'SystemFavicon',
    },
    gover: {
        storeKey: 'GoverStoreKey',
    },
    users: {
        defaultSystemRole: 'users.default_system_role',
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
