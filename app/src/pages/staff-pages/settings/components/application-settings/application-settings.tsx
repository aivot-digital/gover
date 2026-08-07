import {Box, Button, Grid, Tab, Tabs, Typography} from '@mui/material';
import React, {type FormEvent, useEffect, useMemo, useState} from 'react';
import {
    selectSystemConfig,
    setSystemConfigs,
    setSystemConfigsFromMap,
    type SystemConfigMap,
} from '../../../../../slices/system-config-slice';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {TextFieldComponent} from '../../../../../components/text-field/text-field-component';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {SelectFieldComponent} from '../../../../../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../../../../../components/select-field/select-field-component-option';
import {useApi} from '../../../../../hooks/use-api';
import {CheckboxFieldComponent} from '../../../../../components/checkbox-field/checkbox-field-component';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {ThemesApiService} from '../../../../../modules/themes/themes-api-service';
import {SystemConfigsApiService} from '../../../../../modules/configs/system-configs-api-service';
import {
    VDepartmentShadowedApiService,
} from '../../../../../modules/departments/services/v-department-shadowed-api-service';
import {type VDepartmentShadowedEntity} from '../../../../../modules/departments/entities/v-department-shadowed-entity';
import {StorageProvidersApiService} from '../../../../../modules/storage/storage-providers-api-service';
import {StorageProviderType} from '../../../../../modules/storage/enums/storage-provider-type';
import {isStringNullOrEmpty} from '../../../../../utils/string-utils';
import {SystemRolesApiService} from '../../../../../modules/system/services/system-roles-api-service';
import {useConfirm} from '../../../../../providers/confirm-provider';
import {DepartmentSelectField} from '../../../../../modules/departments/components/department-select-field';
import {ModuleIcons} from '../../../../../shells/staff/data/module-icons';
import Label from '@aivot/mui-material-symbols-400-n25-outlined/Label';
import SupervisedUserCircle from '@aivot/mui-material-symbols-400-n25-outlined/SupervisedUserCircle';
import AdminPanelSettings from '@aivot/mui-material-symbols-400-n25-outlined/AdminPanelSettings';
import {
    SystemConfigDefinitionResponseDTO,
} from '../../../../../modules/configs/dtos/system-config-definition-response-dto';
import {ElementDerivationContext} from '../../../../../modules/elements/components/element-derivation-context';
import {ElementType} from '../../../../../data/element-type/element-type';
import {GroupLayout} from '../../../../../models/elements/form/layout/group-layout';
import {ModuleFlag} from '../../../../../utils/module-flags';
import {isApiError} from '../../../../../models/api-error';
import {
    useHasAnyDepartmentPermission,
    useHasSystemPermission,
    useRequireSystemPermission,
} from '../../../../../modules/permissions/hooks/use-permissions';
import {Permission} from '../../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../../../modules/permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';

export function ApplicationSettings() {
    const dispatch = useAppDispatch();
    const api = useApi();
    const confirm = useConfirm();

    useRequireSystemPermission(Permission.SYSTEM_CONFIG_READ);
    const canUpdateSystemConfig = useHasSystemPermission(Permission.SYSTEM_CONFIG_UPDATE);
    const canReadThemes = useHasSystemPermission(Permission.THEME_READ);
    const canReadSystemRoles = useHasSystemPermission(Permission.SYSTEM_ROLE_READ);
    const canReadStorageProviders = useHasSystemPermission(Permission.STORAGE_PROVIDER_READ);
    const canReadDepartments = useHasAnyDepartmentPermission(Permission.DEPARTMENT_READ);
    const updateDisabledTooltip = formatMissingPermissionTooltip(Permission.SYSTEM_CONFIG_UPDATE);
    const themeReadHint = formatMissingPermissionTooltip(Permission.THEME_READ);
    const systemRoleReadHint = formatMissingPermissionTooltip(Permission.SYSTEM_ROLE_READ);
    const storageProviderReadHint = formatMissingPermissionTooltip(Permission.STORAGE_PROVIDER_READ);
    const departmentReadHint = formatMissingPermissionTooltip(Permission.DEPARTMENT_READ);

    const [configDefinitions, setConfigDefinitions] = useState<SystemConfigDefinitionResponseDTO[]>([]);
    useEffect(() => {
        new SystemConfigsApiService(api)
            .listDefinitions()
            .then(setConfigDefinitions);
    }, [api]);

    const config = useAppSelector(selectSystemConfig);
    const [editedConfig, setEditedConfig] = useState<SystemConfigMap>({});

    const [departments, setDepartments] = useState<VDepartmentShadowedEntity[]>([]);
    const [themes, setThemes] = useState<SelectFieldComponentOption[]>([]);
    const [systemRoleOptions, setSystemRoleOptions] = useState<SelectFieldComponentOption[]>([]);
    const [isLoadingSystemRoles, setIsLoadingSystemRoles] = useState(true);
    const [hasSystemRolesLoadingError, setHasSystemRolesLoadingError] = useState(false);

    const [assetStorageProviders, setAssetStorageProviders] = useState<SelectFieldComponentOption[]>([]);
    const [attStorageProviders, setAttStorageProviders] = useState<SelectFieldComponentOption[]>([]);
    const [isLoadingAssetStorageProviders, setIsLoadingAssetStorageProviders] = useState(true);
    const [isLoadingAttStorageProviders, setIsLoadingAttStorageProviders] = useState(true);

    const hasNotChanged = Object.keys(editedConfig).length === 0;
    const configuredDefaultSystemRole = config[SystemConfigKeys.users.defaultSystemRole];
    const configuredMostPrivilegedSystemRole = config[SystemConfigKeys.systemRoles.mostPrivilegedRole];
    const configuredAssetStorageProvider = config[SystemConfigKeys.storage.assets.default_storage_provider];
    const configuredAttachmentStorageProvider = config[SystemConfigKeys.storage.attachments.default_storage_provider];
    const defaultSystemRoleValue = editedConfig[SystemConfigKeys.users.defaultSystemRole] ?? configuredDefaultSystemRole;
    const mostPrivilegedSystemRoleValue = editedConfig[SystemConfigKeys.systemRoles.mostPrivilegedRole] ?? configuredMostPrivilegedSystemRole;
    const assetStorageProviderValue = editedConfig[SystemConfigKeys.storage.assets.default_storage_provider] ?? configuredAssetStorageProvider;
    const attachmentStorageProviderValue = editedConfig[SystemConfigKeys.storage.attachments.default_storage_provider] ?? configuredAttachmentStorageProvider;
    const getConfiguredDepartment = (key: string): VDepartmentShadowedEntity | null => {
        const configuredId = editedConfig[key] ?? config[key];
        const departmentId = configuredId != null && configuredId.length > 0 ? Number(configuredId) : undefined;
        return departmentId != null && Number.isFinite(departmentId)
            ? departments.find((department) => department.id === departmentId) ?? null
            : null;
    };
    const handleChangeListingPageDepartment = (key: string, departmentId: number | null): void => {
        setEditedConfig({
            ...editedConfig,
            [key]: departmentId?.toString() ?? '',
        });
    };
    const defaultSystemRoleError =
        canReadSystemRoles && !isLoadingSystemRoles
            ? systemRoleOptions.length === 0
                ? 'Es ist keine Systemrolle vorhanden. Legen Sie zuerst eine Systemrolle an.'
                : isStringNullOrEmpty(defaultSystemRoleValue)
                    ? 'Bitte wählen Sie eine Standard-Systemrolle aus.'
                    : undefined
            : undefined;
    const mostPrivilegedSystemRoleError =
        canReadSystemRoles && !isLoadingSystemRoles
            ? systemRoleOptions.length === 0
                ? 'Es ist keine Systemrolle vorhanden. Legen Sie zuerst eine Systemrolle an.'
                : isStringNullOrEmpty(mostPrivilegedSystemRoleValue)
                    ? 'Bitte wählen Sie die Systemrolle mit der höchsten Berechtigungsstufe aus.'
                    : undefined
            : undefined;
    const assetStorageProviderError =
        canReadStorageProviders && !isLoadingAssetStorageProviders
            ? assetStorageProviders.length === 0
                ? 'Es ist kein Speicheranbieter für Assets vorhanden. Legen Sie zuerst einen Speicheranbieter an.'
                : isStringNullOrEmpty(assetStorageProviderValue)
                    ? 'Bitte wählen Sie einen Speicheranbieter für Assets aus.'
                    : undefined
            : undefined;
    const attachmentStorageProviderError =
        canReadStorageProviders && !isLoadingAttStorageProviders
            ? attStorageProviders.length === 0
                ? 'Es ist kein Speicheranbieter für Vorgangsanlagen vorhanden. Legen Sie zuerst einen Speicheranbieter an.'
                : isStringNullOrEmpty(attachmentStorageProviderValue)
                    ? 'Bitte wählen Sie einen Speicheranbieter für Vorgangsanlagen aus.'
                    : undefined
            : undefined;

    // These dependent option lists are optional for read-only access to system settings.
    // Skipping unauthorized lookups prevents unrelated 403 toasts while keeping the page readable.
    useEffect(() => {
        if (!canReadThemes) {
            setThemes([]);
            return;
        }

        let isActive = true;

        new ThemesApiService(api)
            .listAll()
            .then((themes) => {
                if (isActive) {
                    setThemes(themes.content.map((theme) => ({
                        value: theme.id.toString(),
                        label: theme.name,
                    })));
                }
            })
            .catch((err) => {
                if (isActive) {
                    console.error(err);
                    dispatch(showErrorSnackbar('Erscheinungsbilder konnten nicht geladen werden'));
                }
            });

        return () => {
            isActive = false;
        };
    }, [api, canReadThemes, dispatch]);

    useEffect(() => {
        if (!canReadSystemRoles) {
            setSystemRoleOptions([]);
            setHasSystemRolesLoadingError(false);
            setIsLoadingSystemRoles(false);
            return;
        }

        let isActive = true;
        setIsLoadingSystemRoles(true);
        setHasSystemRolesLoadingError(false);

        new SystemRolesApiService()
            .listAll()
            .then((roles) => {
                if (isActive) {
                    setSystemRoleOptions(roles.content
                        .map((role) => ({
                            value: role.id.toString(),
                            label: role.name,
                            subLabel: role.description ?? undefined,
                        }))
                        .sort((a, b) => a.label.localeCompare(b.label)));
                }
            })
            .catch((err) => {
                if (isActive) {
                    setHasSystemRolesLoadingError(true);
                    dispatch(showApiErrorSnackbar(err, 'Die Liste der Systemrollen konnte nicht geladen werden'));
                }
            })
            .finally(() => {
                if (isActive) {
                    setIsLoadingSystemRoles(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadSystemRoles, dispatch]);

    useEffect(() => {
        if (!canReadStorageProviders) {
            setAssetStorageProviders([]);
            setIsLoadingAssetStorageProviders(false);
            return;
        }

        let isActive = true;
        setIsLoadingAssetStorageProviders(true);

        new StorageProvidersApiService()
            .listAll({
                type: StorageProviderType.Assets,
            })
            .then(({content: providers}) => {
                if (isActive) {
                    setAssetStorageProviders(providers.map((prv) => ({
                        value: prv.id.toString(),
                        label: prv.name,
                        subLabel: prv.description,
                    })));
                }
            })
            .catch((err) => {
                if (isActive) {
                    dispatch(showApiErrorSnackbar(err, 'Die Liste der Speicheranbieter für Assets konnte nicht geladen werden'));
                }
            })
            .finally(() => {
                if (isActive) {
                    setIsLoadingAssetStorageProviders(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadStorageProviders, dispatch]);

    useEffect(() => {
        if (!canReadStorageProviders) {
            setAttStorageProviders([]);
            setIsLoadingAttStorageProviders(false);
            return;
        }

        let isActive = true;
        setIsLoadingAttStorageProviders(true);

        new StorageProvidersApiService()
            .listAll({
                type: StorageProviderType.Attachments,
            })
            .then(({content: providers}) => {
                if (isActive) {
                    setAttStorageProviders(providers.map((prv) => ({
                        value: prv.id.toString(),
                        label: prv.name,
                        subLabel: prv.description,
                    })));
                }
            })
            .catch((err) => {
                if (isActive) {
                    dispatch(showApiErrorSnackbar(err, 'Die Liste der Speicheranbieter konnte nicht geladen werden'));
                }
            })
            .finally(() => {
                if (isActive) {
                    setIsLoadingAttStorageProviders(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadStorageProviders, dispatch]);

    useEffect(() => {
        if (!canUpdateSystemConfig || attStorageProviders.length === 0) {
            return;
        }

        setEditedConfig((prev) => {
            const currentValue =
                prev[SystemConfigKeys.storage.attachments.default_storage_provider] ??
                configuredAttachmentStorageProvider;

            if (!isStringNullOrEmpty(currentValue)) {
                return prev;
            }

            return {
                ...prev,
                [SystemConfigKeys.storage.attachments.default_storage_provider]: attStorageProviders[0].value,
            };
        });
    }, [attStorageProviders, canUpdateSystemConfig, configuredAttachmentStorageProvider]);

    useEffect(() => {
        if (!canUpdateSystemConfig || assetStorageProviders.length === 0) {
            return;
        }

        setEditedConfig((prev) => {
            const currentValue =
                prev[SystemConfigKeys.storage.assets.default_storage_provider] ??
                configuredAssetStorageProvider;

            if (!isStringNullOrEmpty(currentValue)) {
                return prev;
            }

            return {
                ...prev,
                [SystemConfigKeys.storage.assets.default_storage_provider]: assetStorageProviders[0].value,
            };
        });
    }, [assetStorageProviders, canUpdateSystemConfig, configuredAssetStorageProvider]);

    const handleSubmit = async (event: FormEvent): Promise<void> => {
        event.preventDefault();

        if (canUpdateSystemConfig && editedConfig != null) {
            const normalizedEditedConfig = {
                ...editedConfig,
            };

            const appliedStorageProviderDefaults: SystemConfigMap = {};
            const requiredStorageProviderConfigs = [
                {
                    key: SystemConfigKeys.storage.attachments.default_storage_provider,
                    options: attStorageProviders,
                    canRead: canReadStorageProviders,
                    errorMessage: 'Für Vorgangsanlagen muss ein zentraler Speicheranbieter vorhanden sein. Legen Sie zuerst einen Speicheranbieter an.',
                },
                {
                    key: SystemConfigKeys.storage.assets.default_storage_provider,
                    options: assetStorageProviders,
                    canRead: canReadStorageProviders,
                    errorMessage: 'Für Assets muss ein zentraler Speicheranbieter vorhanden sein. Legen Sie zuerst einen Speicheranbieter an.',
                },
            ];

            for (const requiredStorageProviderConfig of requiredStorageProviderConfigs) {
                if (!requiredStorageProviderConfig.canRead) {
                    continue;
                }

                const currentValue =
                    normalizedEditedConfig[requiredStorageProviderConfig.key] ??
                    config[requiredStorageProviderConfig.key];

                if (!isStringNullOrEmpty(currentValue)) {
                    continue;
                }

                if (requiredStorageProviderConfig.options.length === 0) {
                    dispatch(showErrorSnackbar(requiredStorageProviderConfig.errorMessage));
                    return;
                }

                const fallbackProviderValue = requiredStorageProviderConfig.options[0].value;
                normalizedEditedConfig[requiredStorageProviderConfig.key] = fallbackProviderValue;
                appliedStorageProviderDefaults[requiredStorageProviderConfig.key] = fallbackProviderValue;
            }

            const currentDefaultSystemRole =
                normalizedEditedConfig[SystemConfigKeys.users.defaultSystemRole] ??
                config[SystemConfigKeys.users.defaultSystemRole];

            if (canReadSystemRoles && isStringNullOrEmpty(currentDefaultSystemRole)) {
                if (systemRoleOptions.length === 0) {
                    dispatch(showErrorSnackbar('Für automatische Benutzerimporte muss mindestens eine Systemrolle vorhanden sein. Legen Sie zuerst eine Systemrolle an.'));
                    return;
                }

                dispatch(showErrorSnackbar('Bitte wählen Sie eine Standard-Systemrolle für automatische Benutzerimporte aus.'));
                return;
            }

            const currentMostPrivilegedSystemRole =
                normalizedEditedConfig[SystemConfigKeys.systemRoles.mostPrivilegedRole] ??
                config[SystemConfigKeys.systemRoles.mostPrivilegedRole];

            if (canReadSystemRoles && isStringNullOrEmpty(currentMostPrivilegedSystemRole)) {
                if (systemRoleOptions.length === 0) {
                    dispatch(showErrorSnackbar('Für die höchste Berechtigungsstufe muss mindestens eine Systemrolle vorhanden sein. Legen Sie zuerst eine Systemrolle an.'));
                    return;
                }

                dispatch(showErrorSnackbar('Bitte wählen Sie die Systemrolle mit der höchsten Berechtigungsstufe aus.'));
                return;
            }

            if (Object.keys(appliedStorageProviderDefaults).length > 0) {
                setEditedConfig((prev) => ({
                    ...prev,
                    ...appliedStorageProviderDefaults,
                }));
            }

            const updatedConfigs = Object
                .keys(normalizedEditedConfig)
                .filter((key) => normalizedEditedConfig[key] !== config[key])
                .map((key) => ({
                    key,
                    value: normalizedEditedConfig[key],
                }));

            const attachmentStorageProviderKey = SystemConfigKeys.storage.attachments.default_storage_provider;
            const attachmentStorageProviderConfig = updatedConfigs
                .find((config) => config.key === attachmentStorageProviderKey);
            const regularConfigs = updatedConfigs
                .filter((config) => config.key !== attachmentStorageProviderKey);
            const saveConfig = (config: { key: string; value: string }, changeConfirmed: boolean = false) => {
                return new SystemConfigsApiService(api).update(config.key, {
                    value: config.value,
                    changeConfirmed: changeConfirmed ? true : undefined,
                });
            };

            try {
                const savedAttachmentConfigs: Awaited<ReturnType<typeof saveConfig>>[] = [];

                if (attachmentStorageProviderConfig != null) {
                    try {
                        savedAttachmentConfigs.push(await saveConfig(attachmentStorageProviderConfig));
                    } catch (err) {
                        if (!isApiError(err) || err.status !== 409) {
                            throw err;
                        }

                        const attachmentStorageProviderChangeConfirmed = await confirm({
                            title: 'Speicheranbieter für Vorgangsanlagen ändern',
                            confirmButtonText: 'Speicheranbieter ändern',
                            width: 'md',
                            children: (
                                <>
                                    <Typography sx={{mb: 1}}>
                                        Das System hat laufende Vorgänge registriert. Der zentrale Speicheranbieter für
                                        Vorgangsanlagen sollte in diesem Zustand nur bewusst geändert werden.
                                    </Typography>
                                    <Typography sx={{mb: 1}}>
                                        Durch den Wechsel können Datenstrukturen auf mehrere Speicheranbieter aufgeteilt
                                        werden. Außerdem kann der Zugriff auf bereits bestehende Anhänge innerhalb
                                        aktiver Vorgänge verloren gehen.
                                    </Typography>
                                    <Typography>
                                        Möchten Sie den Speicheranbieter trotzdem ändern?
                                    </Typography>
                                </>
                            ),
                        });

                        if (!attachmentStorageProviderChangeConfirmed) {
                            return;
                        }

                        savedAttachmentConfigs.push(await saveConfig(attachmentStorageProviderConfig, true));
                    }
                }

                const configs = [
                    ...savedAttachmentConfigs,
                    ...await Promise.all(regularConfigs.map((config) => saveConfig(config))),
                ];

                dispatch(showSuccessSnackbar('Einstellungen erfolgreich gespeichert'));
                dispatch(setSystemConfigs(configs));

                const newThemeId =
                    editedConfig[SystemConfigKeys.system.theme] ??
                    config[SystemConfigKeys.system.theme];

                const oldThemeId = config[SystemConfigKeys.system.theme];

                if (newThemeId != null && newThemeId !== oldThemeId) {
                    confirm({
                        title: 'Änderungen ausstehend',
                        children: (
                            <Typography>
                                Die Änderungen am Erscheinungsbild werden erst nach einem "Neu Laden" der Anwendung
                                aktiv.
                            </Typography>
                        ),
                        hideCancelButton: true,
                    });
                }

                setEditedConfig({});
            } catch (err) {
                console.error(err);
                dispatch(showApiErrorSnackbar(err, 'Einstellungen konnten nicht gespeichert werden'));
            }
        }
    };

    useEffect(() => {
        if (!canReadDepartments) {
            setDepartments([]);
            return;
        }

        let isActive = true;

        new VDepartmentShadowedApiService()
            .listAll({includeAncestors: true})
            .then(deps => {
                if (isActive) {
                    setDepartments(deps.content);
                }
            })
            .catch((err) => {
                if (isActive) {
                    console.error(err);
                    dispatch(showErrorSnackbar('Die Liste der Organisationseinheiten konnte nicht geladen werden'));
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadDepartments, dispatch]);

    const [currentSettingsTab, setCurrentSettingsTab] = useState<string>();
    const availableTabs = useMemo(() => configDefinitions
        .reduce((acc, cur) => {
            if (!acc.includes(cur.category)) {
                acc.push(cur.category);
            }
            return acc;
        }, [] as string[])
        .sort((a, b) => a.localeCompare(b)), [configDefinitions]);
    const groups: Record<string, GroupLayout> = useMemo(() => {
        return availableTabs
            .reduce((acc, category) => {
                const configsChildren = configDefinitions
                    .filter((def) => def.category === category)
                    .map((def) => def.configElement);

                acc[category] = {
                    id: category,
                    type: ElementType.GroupLayout,
                    children: configsChildren,
                    name: null,
                    storeLink: null,
                    testProtocolSet: null,
                    weight: null,
                    visibility: null,
                    override: null,
                    metadata: null,
                };
                return acc;
            }, {} as Record<string, GroupLayout>);
    }, [availableTabs]);
    const currentGroup: GroupLayout | undefined = useMemo(() => {
        return groups[currentSettingsTab ?? availableTabs[0]];
    }, [groups, currentSettingsTab, availableTabs]);

    if (localStorage.getItem('showNewSettings') != null) {
        return (
            <Box
                sx={{
                    marginTop: 3.5,
                }}
            >
                <Tabs
                    value={currentSettingsTab ?? availableTabs[0] ?? ''}
                    onChange={(_, value) => {
                        console.log(value);
                        setCurrentSettingsTab(value);
                    }}
                >
                    {
                        availableTabs.map((category) => (
                            <Tab
                                key={category}
                                label={category}
                                value={category}
                            />
                        ))
                    }
                </Tabs>

                <Box
                    sx={{
                        paddingX: 2,
                        paddingTop: 1,
                        paddingBottom: 2,
                    }}
                >
                    {
                        currentGroup != null &&
                        <ElementDerivationContext
                            element={currentGroup}
                            authoredElementValues={config}
                            disabled={!canUpdateSystemConfig}
                            onAuthoredElementValuesChange={(updated) => {
                                console.log(updated);
                                dispatch(setSystemConfigsFromMap(updated));
                            }}
                        />
                    }

                    <Box
                        sx={{
                            mt: 4,
                        }}
                    >
                        <DisabledTooltip
                            disabled={!canUpdateSystemConfig}
                            title={updateDisabledTooltip}
                        >
                            <Button
                                type="submit"
                                disabled={hasNotChanged || !canUpdateSystemConfig}
                                color="primary"
                                variant="contained"
                                startIcon={<SaveOutlinedIcon
                                    sx={{
                                        marginTop: '-2px',
                                    }}
                                />}
                            >
                                Speichern
                            </Button>
                        </DisabledTooltip>

                        <Button
                            sx={{
                                ml: 2,
                            }}
                            type="button"
                            color="error"
                            disabled={hasNotChanged || !canUpdateSystemConfig}
                            onClick={() => {
                                setEditedConfig({});
                            }}
                        >
                            Zurücksetzen
                        </Button>
                    </Box>
                </Box>
            </Box>
        );
    }

    return (
        <Box
            sx={{
                marginTop: 3.5,
                padding: 2,
            }}
        >
            <form onSubmit={handleSubmit}>
                <Typography
                    variant="subtitle1"
                >
                    Über den Betreiber
                </Typography>
                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Hinterlegen Sie grundsätzliche Informationen über den Betreiber dieses Systems.
                    Diese Informationen werden in der Anwendung angezeigt und sind für die Nutzer:innen sichtbar.
                    Änderungen am Betreiber-Namen werden erst nach dem nächsten Neu-Laden der Anwendung in allen
                    Bereichen
                    sichtbar.
                </Typography>
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Name des Betreibers"
                            placeholder="Bad Musterstadt"
                            value={editedConfig[SystemConfigKeys.provider.name] ?? config[SystemConfigKeys.provider.name]}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.provider.name]: val ?? '',
                                });
                            }}
                            required
                            disabled={!canUpdateSystemConfig}
                            startIcon={<Label/>}
                        />
                    </Grid>
                </Grid>
                {
                    themes.length > 0 &&
                    <>
                        <Typography
                            variant="subtitle1"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Erscheinungsbild der Prosuna-Instanz
                        </Typography>

                        <Typography
                            sx={{
                                maxWidth: 900,
                                mb: 1.6,
                            }}
                        >
                            Sie können ein eigenes Erscheinungsbild für die Benutzeroberfläche auswählen, um Prosuna an
                            Ihr
                            Corporate Design anzugleichen (wird z.B. verwendet für Administrationsoberfläche und die
                            Index-Seite der veröffentlichten
                            Formulare).
                        </Typography>

                        <Grid
                            container
                            columnSpacing={4}
                        >
                            <Grid
                                size={{
                                    xs: 12,
                                    lg: 6,
                                }}
                            >
                                <SelectFieldComponent
                                    label="Erscheinungsbild"
                                    options={themes}
                                    value={editedConfig[SystemConfigKeys.system.theme] ?? config[SystemConfigKeys.system.theme]}
                                    onChange={(val) => {
                                        setEditedConfig({
                                            ...editedConfig,
                                            [SystemConfigKeys.system.theme]: val ?? '',
                                        });
                                    }}
                                    disabled={!canUpdateSystemConfig || !canReadThemes}
                                    hint={!canReadThemes ? themeReadHint : undefined}
                                    startIcon={ModuleIcons.themes}
                                />
                            </Grid>
                        </Grid>
                    </>
                }

                <Typography
                    variant="subtitle1"
                    sx={{
                        mt: 4,
                    }}
                >
                    Prosuna Store
                </Typography>
                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Im Prosuna Store finden Sie Bausteine und Formulare zur Nachnutzung. Wenn Sie eigene Formulare
                    und/oder
                    Bausteine im Prosuna Store zur Verfügung stellen möchten, benötigen Sie einen eigenen Schlüssel
                    (API-Key).
                </Typography>
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Schlüssel für den Prosuna Store"
                            placeholder="b721fe43-5800-40a3-ae7f-d19274dd72f1"
                            hint="Geben Sie hier Ihren Schlüssel für den Prosuna Store ein, wenn Sie eigene Formulare und/oder Vorlagen im Prosuna Store veröffentlichen wollen."
                            value={editedConfig[SystemConfigKeys.prosuna.storeKey] ?? config[SystemConfigKeys.prosuna.storeKey]}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.prosuna.storeKey]: val ?? '',
                                });
                            }}
                            disabled={!canUpdateSystemConfig}
                            startIcon={ModuleIcons.secrets}
                        />
                    </Grid>
                </Grid>

                <Typography
                    variant="subtitle1"
                    sx={{
                        mt: 4,
                    }}
                >
                    Systemrollen
                </Typography>
                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Legen Sie fest, welche Systemrollen bei automatischen Benutzerimporten verwendet werden und welche
                    Rolle systemweit als höchste Berechtigungsstufe gilt.
                </Typography>
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Standard-Systemrolle für automatische Benutzerimporte"
                            hint={
                                !canReadSystemRoles
                                    ? systemRoleReadHint
                                    : hasSystemRolesLoadingError
                                    ? 'Die Systemrollen konnten nicht geladen werden. Bitte laden Sie die Seite neu oder prüfen Sie Ihre Berechtigungen.'
                                    : 'Diese Systemrolle wird bei neuen automatischen Benutzerimporten und -synchronisationen verwendet.'
                            }
                            value={defaultSystemRoleValue}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.users.defaultSystemRole]: val ?? '',
                                });
                            }}
                            required
                            error={defaultSystemRoleError}
                            disabled={!canUpdateSystemConfig || !canReadSystemRoles || isLoadingSystemRoles}
                            options={systemRoleOptions}
                            emptyStatePlaceholder={
                                !canReadSystemRoles
                                    ? 'Keine Berechtigung zur Einsicht'
                                    : isLoadingSystemRoles
                                    ? 'Systemrollen werden geladen…'
                                    : hasSystemRolesLoadingError
                                        ? 'Systemrollen konnten nicht geladen werden'
                                        : 'Keine Systemrollen vorhanden'
                            }
                            startIcon={<SupervisedUserCircle/>}
                        />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Systemrolle mit höchster Berechtigungsstufe"
                            hint={
                                !canReadSystemRoles
                                    ? systemRoleReadHint
                                    : hasSystemRolesLoadingError
                                    ? 'Die Systemrollen konnten nicht geladen werden. Bitte laden Sie die Seite neu oder prüfen Sie Ihre Berechtigungen.'
                                    : 'Diese Systemrolle gilt in Prosuna als höchste Berechtigungsstufe. Besitzt keine aktive Mitarbeiter:in diese Rolle, wird sie automatisch dem Administrationskonto zugewiesen, dessen E-Mail-Adresse über die Umgebungsvariable PROSUNA_BOOTSTRAP_ADMIN_MAIL konfiguriert ist.'
                            }
                            value={mostPrivilegedSystemRoleValue}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.systemRoles.mostPrivilegedRole]: val ?? '',
                                });
                            }}
                            required
                            error={mostPrivilegedSystemRoleError}
                            disabled={!canUpdateSystemConfig || !canReadSystemRoles || isLoadingSystemRoles}
                            options={systemRoleOptions}
                            emptyStatePlaceholder={
                                !canReadSystemRoles
                                    ? 'Keine Berechtigung zur Einsicht'
                                    : isLoadingSystemRoles
                                    ? 'Systemrollen werden geladen…'
                                    : hasSystemRolesLoadingError
                                        ? 'Systemrollen konnten nicht geladen werden'
                                        : 'Keine Systemrollen vorhanden'
                            }
                            startIcon={<AdminPanelSettings/>}
                        />
                    </Grid>
                </Grid>

                <Typography
                    variant="subtitle1"
                    sx={{
                        mt: 4,
                    }}
                >
                    Zentraler Speicheranbieter für Vorgangsanlagen
                </Typography>
                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Dieser Speicheranbieter wird verwendet, um Vorgangsanlagen zu speichern, wenn kein spezifischer
                    Speicheranbieter innerhalb eines Prozesselementes konfiguriert ist.
                    Bitte beachten Sie, dass die Änderung dieses Schlüssels Auswirkungen auf alle Vorgänge hat, die den
                    zentralen Speicheranbieter verwenden.
                </Typography>
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Zentraler Speicheranbieter für Vorgangsanlagen"
                            hint={!canReadStorageProviders ? storageProviderReadHint : 'Geben Sie den Speicheranbieter an, der standardmäßig für Vorgangsanlagen verwendet werden soll.'}
                            value={attachmentStorageProviderValue}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.storage.attachments.default_storage_provider]: val ?? '',
                                });
                            }}
                            required
                            error={attachmentStorageProviderError}
                            disabled={!canUpdateSystemConfig || !canReadStorageProviders || isLoadingAttStorageProviders}
                            options={attStorageProviders}
                            emptyStatePlaceholder={!canReadStorageProviders ? 'Keine Berechtigung zur Einsicht' : 'Keine Speicheranbieter für Vorgangsanlagen vorhanden'}
                            startIcon={ModuleIcons.storage}
                        />
                    </Grid>
                </Grid>

                <Typography
                    variant="subtitle1"
                    sx={{
                        mt: 4,
                    }}
                >
                    Zentraler Speicheranbieter für Assets
                </Typography>
                <Typography
                    sx={{
                        maxWidth: 900,
                        mb: 1.6,
                    }}
                >
                    Dieser Speicheranbieter wird verwendet, um Assets zu speichern, wenn kein spezifischer
                    Speicheranbieter
                    für ein Asset konfiguriert ist.
                    Bitte beachten Sie, dass die Änderung dieses Schlüssels Auswirkungen auf alle Assets hat, die den
                    zentralen Speicheranbieter verwenden.
                </Typography>
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Zentraler Speicheranbieter für Assets"
                            hint={!canReadStorageProviders ? storageProviderReadHint : 'Geben Sie den Speicheranbieter an, der standardmäßig für Assets verwendet werden soll.'}
                            value={assetStorageProviderValue}
                            onChange={(val) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.storage.assets.default_storage_provider]: val ?? '',
                                });
                            }}
                            required
                            error={assetStorageProviderError}
                            disabled={!canUpdateSystemConfig || !canReadStorageProviders || isLoadingAssetStorageProviders}
                            options={assetStorageProviders}
                            emptyStatePlaceholder={!canReadStorageProviders ? 'Keine Berechtigung zur Einsicht' : 'Keine Speicheranbieter für Assets vorhanden'}
                            startIcon={ModuleIcons.storage}
                        />
                    </Grid>
                </Grid>

                {
                    AppConfig.moduleFlags.includes(ModuleFlag.Form) &&
                    <>
                        <Typography
                            variant="h6"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Öffentliche Auflistung der veröffentlichten Formulare (Index-Seite)
                        </Typography>
                        <Typography
                            sx={{
                                maxWidth: 900,
                                mb: 1.6,
                            }}
                        >
                            Wenn die Domain des Systems direkt aufgerufen wird, wird eine öffentliche Index-Seite
                            angezeigt, die alle veröffentlichten Formulare auflistet. Hier können Sie diese Seite konfigurieren
                            und
                            ggf. deaktivieren.
                        </Typography>
                        <Grid
                            container
                            columnSpacing={4}
                        >
                            <Grid
                                size={{
                                    xs: 12,
                                    lg: 4,
                                }}
                            >
                                <DepartmentSelectField
                                    label="Text für das Impressum"
                                    value={getConfiguredDepartment(SystemConfigKeys.provider.listingPage.imprintDepartmentId)}
                                    onChange={(department) => {
                                        handleChangeListingPageDepartment(SystemConfigKeys.provider.listingPage.imprintDepartmentId, department?.id ?? null);
                                    }}
                                    disabled={!canUpdateSystemConfig || !canReadDepartments}
                                    hint={!canReadDepartments ? departmentReadHint : undefined}
                                />

                            </Grid>
                            <Grid
                                size={{
                                    xs: 12,
                                    lg: 4,
                                }}
                            >
                                <DepartmentSelectField
                                    label="Text für die Datenschutzerklärung"
                                    value={getConfiguredDepartment(SystemConfigKeys.provider.listingPage.privacyDepartmentId)}
                                    onChange={(department) => {
                                        handleChangeListingPageDepartment(SystemConfigKeys.provider.listingPage.privacyDepartmentId, department?.id ?? null);
                                    }}
                                    disabled={!canUpdateSystemConfig || !canReadDepartments}
                                    hint={!canReadDepartments ? departmentReadHint : undefined}
                                />
                            </Grid>
                            <Grid
                                size={{
                                    xs: 12,
                                    lg: 4,
                                }}
                            >
                                <DepartmentSelectField
                                    label="Text für die Erklärung der Barrierefreiheit"
                                    value={getConfiguredDepartment(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId)}
                                    onChange={(department) => {
                                        handleChangeListingPageDepartment(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId, department?.id ?? null);
                                    }}
                                    disabled={!canUpdateSystemConfig || !canReadDepartments}
                                    hint={!canReadDepartments ? departmentReadHint : undefined}
                                />
                            </Grid>
                        </Grid>
                        <Typography
                            variant="caption"
                            color={'text.secondary'}
                        >
                            Rechtstexte werden auf Ebene der Organisationseinheiten hinterlegt und verwaltet. Sie können hier
                            die
                            Organisationseinheiten auswählen, deren Texte Sie verwenden und anzeigen möchten.
                        </Typography>
                        <CheckboxFieldComponent
                            label="Öffentliche Auflistung der veröffentlichten Formulare (in Form einer Index-Seite) vollständig deaktivieren"
                            value={(editedConfig[SystemConfigKeys.provider.listingPage.disableProsunaListingPage] ?? config[SystemConfigKeys.provider.listingPage.disableProsunaListingPage]) == 'true'}
                            onChange={(checked) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.provider.listingPage.disableProsunaListingPage]: checked ? 'true' : '',
                                });
                            }}
                            hint="Bitte nehmen Sie zur Kenntnis, dass dies die Barrierefreiheit und Zugänglichkeit Ihrer Formulare beeinträchtigen kann."
                            disabled={!canUpdateSystemConfig}
                        />
                        <Typography
                            variant="subtitle1"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Verweis auf Formular-Index aus Formularen heraus
                        </Typography>
                        <Typography
                            sx={{
                                maxWidth: 900,
                                mb: 1.6,
                            }}
                        >
                            Am Ende eines jeden Formulars wird Ihre Index-Seite mit dem Text „Weitere Formulare“ verlinkt.
                            Diese Verlinkung dient der Barrierefreiheit
                            (gemäß <abbr title={'Web Content Accessibility Guidelines'}>WCAG</abbr> 2.1)
                            und der Zugänglichkeit Ihrer Formulare. Sie können diesen Link deaktivieren oder gegen einen eigenen
                            Link ersetzen
                            (wenn Sie zum Beispiel alle Formulare auf Ihrer eigenen Webseite auflisten).
                        </Typography>
                        {
                            (editedConfig[SystemConfigKeys.provider.listingPage.disableListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.disableListingPageLink]) != 'true' &&
                            <Box>
                                <TextFieldComponent
                                    label="Link zu externer Formular-Auflistung"
                                    placeholder="https://bad-musterstadt.de/formulare"
                                    hint="Der Link wird (soweit angegeben) anstelle des regulären Links mit dem Text „Weitere Formulare“ am Ende eines jeden Formulars angezeigt."
                                    value={editedConfig[SystemConfigKeys.provider.listingPage.customListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.customListingPageLink]}
                                    pattern={{
                                        regex: '^(https?://)([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$',
                                        message: 'Bitte geben Sie eine gültige URL ein (z.B. https://bad-musterstadt.de/formulare).',
                                    }}
                                    onChange={(val) => {
                                        setEditedConfig({
                                            ...editedConfig,
                                            [SystemConfigKeys.provider.listingPage.customListingPageLink]: val ?? '',
                                        });
                                    }}
                                    disabled={!canUpdateSystemConfig}
                                    startIcon={ModuleIcons.providerLinks}
                                />
                            </Box>
                        }
                        <CheckboxFieldComponent
                            label="Verlinkung von Formularen zur Formular-Index-Seite vollständig deaktivieren"
                            value={(editedConfig[SystemConfigKeys.provider.listingPage.disableListingPageLink] ?? config[SystemConfigKeys.provider.listingPage.disableListingPageLink]) == 'true'}
                            onChange={(checked) => {
                                setEditedConfig({
                                    ...editedConfig,
                                    [SystemConfigKeys.provider.listingPage.disableListingPageLink]: checked ? 'true' : 'false',
                                });
                            }}
                            hint="Bitte nehmen Sie zur Kenntnis, dass dies die Barrierefreiheit und Zugänglichkeit Ihrer Formulare beeinträchtigen kann."
                            disabled={!canUpdateSystemConfig}
                        />
                    </>
                }
                <Box
                    sx={{
                        mt: 4,
                    }}
                >
                    <DisabledTooltip
                        disabled={!canUpdateSystemConfig}
                        title={updateDisabledTooltip}
                    >
                        <Button
                            type="submit"
                            disabled={hasNotChanged || !canUpdateSystemConfig}
                            color="primary"
                            variant="contained"
                            startIcon={<SaveOutlinedIcon
                                sx={{
                                    marginTop: '-2px',
                                }}
                            />}
                        >
                            Speichern
                        </Button>
                    </DisabledTooltip>

                    <Button
                        sx={{
                            ml: 2,
                        }}
                        type="button"
                        color="error"
                        disabled={hasNotChanged || !canUpdateSystemConfig}
                        onClick={() => {
                            setEditedConfig({});
                        }}
                    >
                        Zurücksetzen
                    </Button>
                </Box>
            </form>

        </Box>
    );
}
