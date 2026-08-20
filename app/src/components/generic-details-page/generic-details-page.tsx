import {
    type GenericDetailsPagePermissionConfig,
    type GenericDetailsPagePermissionScope,
    type GenericDetailsPageProps,
    type TabConfig,
} from './generic-details-page-props';
import {Box, Button, Container, Paper, Stack, Tab, Tabs, Typography} from '@mui/material';
import React, {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Api, useApi} from '../../hooks/use-api';
import {GenericPageHeader} from '../generic-page-header/generic-page-header';
import {generatePath, Link, matchPath, Outlet, useLocation, useNavigate, useParams} from 'react-router-dom';
import {GenericDetailsPageContext} from './generic-details-page-context';
import {ApiError, isApiError} from '../../models/api-error';
import NotFoundIllustration from './resource-not-found-illustration.svg?react';
import ArrowBackOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ArrowBack';
import FormatListBulletedOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/FormatListBulleted';
import {DisabledTooltip} from '../disabled-tooltip/disabled-tooltip';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectPermissions} from '../../slices/user-slice';
import {
    hasSystemPermission,
    hasScopedPermission,
    createPermissionDeniedError,
    formatMissingPermissionTooltip,
    type PermissionLike,
    resolvePermissionRequirement,
} from '../../modules/permissions/utils/permission-utils';
import {type PermissionSet} from '../../modules/permissions/models/permission-set';
import {SearchItemService} from '../../modules/search/search-item-service';

export const DEFAULT_ID_PARAM = 'id';
export const NEW_ID_INDICATOR = 'new';

interface DataFetchResult<ItemType, AdditionalData> {
    item: ItemType;
    additionalData: AdditionalData;
}

type ResolvedTabState = {
    disabled: boolean;
    disabledByOnlyExisting: boolean;
    missingPermission?: PermissionLike;
    tooltip?: ReactNode;
};

async function fetchData<ItemType, ID, AdditionalData>(api: Api, id: ID, props: GenericDetailsPageProps<ItemType, ID, AdditionalData>): Promise<DataFetchResult<ItemType, AdditionalData>> {
    let item: ItemType;
    if (id === NEW_ID_INDICATOR) {
        item = props.initializeItem(api);
    } else {
        item = await props.fetchData(api, id);
    }

    let additionalData: Partial<AdditionalData> = {};
    if (props.fetchAdditionalData != null) {
        for (const key in props.fetchAdditionalData) {
            additionalData[key] = await props.fetchAdditionalData[key](api, id);
        }
    }

    return Promise.resolve({
        item: item,
        additionalData: additionalData as AdditionalData,
    });
}

function resolveTabState<ItemType>(
    tab: TabConfig<ItemType>,
    item: ItemType | undefined,
    isNewItem: boolean,
    permissionSet: PermissionSet | undefined,
    defaultScope: GenericDetailsPagePermissionScope<ItemType> | undefined,
): ResolvedTabState {
    const disabledByOnlyExisting = tab.onlyExisting === true && (isNewItem || item == null);
    const disabledByCustomCheck = tab.isDisabled?.(item) ?? false;
    const requiredPermission = resolvePermissionRequirement(tab.requiredPermission, defaultScope);
    const missingPermission = !disabledByOnlyExisting && requiredPermission != null && !hasScopedPermission(permissionSet, item, requiredPermission.scope, requiredPermission.permission)
        ? requiredPermission.permission
        : undefined;
    const explicitTooltip = typeof tab.disabledTooltip === 'function'
        ? tab.disabledTooltip(item)
        : tab.disabledTooltip;

    return {
        disabled: disabledByOnlyExisting || disabledByCustomCheck || missingPermission != null,
        disabledByOnlyExisting: disabledByOnlyExisting,
        missingPermission: missingPermission,
        tooltip: explicitTooltip ??
            (missingPermission != null ? formatMissingPermissionTooltip(missingPermission) : undefined) ??
            (disabledByOnlyExisting ? 'Dieser Tab ist erst nach dem Anlegen verfügbar.' : undefined),
    };
}

function ensureConfiguredAccess<ItemType>(
    item: ItemType | undefined,
    isNewItem: boolean,
    permissionSet: PermissionSet | undefined,
    permissionConfig: GenericDetailsPagePermissionConfig<ItemType> | undefined,
): void {
    if (permissionConfig == null) {
        return;
    }

    if (isNewItem) {
        // New-resource pages must fail on the create permission before any fetchAdditionalData call can
        // surface a broader read permission error from lookup data needed by the form.
        const permission = permissionConfig.create;
        if (permission != null && !hasSystemPermission(permissionSet, permission)) {
            throw createPermissionDeniedError(permission);
        }
        return;
    }

    if (item == null) {
        return;
    }

    const permission = permissionConfig.read;
    if (permission == null) {
        return;
    }

    if (!hasScopedPermission(permissionSet, item, permissionConfig.scope, permission)) {
        throw createPermissionDeniedError(permission);
    }
}

function checkConfiguredEditability<ItemType>(
    item: ItemType | undefined,
    isNewItem: boolean,
    permissionSet: PermissionSet | undefined,
    permissionConfig: GenericDetailsPagePermissionConfig<ItemType> | undefined,
): boolean {
    if (permissionConfig == null) {
        return true;
    }

    if (item == null) {
        return false;
    }

    const permission = isNewItem ? permissionConfig.create : permissionConfig.update;
    if (permission == null) {
        return true;
    }

    return isNewItem
        ? hasSystemPermission(permissionSet, permission)
        : hasScopedPermission(permissionSet, item, permissionConfig.scope, permission);
}

export function GenericDetailsPage<ItemType, ID, AdditionalData>(props: GenericDetailsPageProps<ItemType, ID, AdditionalData>) {
    const {
        entityType,
        isEditable,
        permissionCheck,
    } = props;

    const api = useApi();
    const params = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [notFound, setNotFound] = useState(false);
    const [loadError, setLoadError] = useState<ApiError>();
    const permissionSet = useAppSelector(selectPermissions);

    const ID_PARAM = props.idParam ?? DEFAULT_ID_PARAM;
    const id = useMemo(() => {
        return params[ID_PARAM] as ID;
    }, [params]);
    const isNewItem = id === NEW_ID_INDICATOR;

    const [isBusy, setIsBusy] = useState(false);
    const [item, setItem] = useState<ItemType>();
    const [additionalData, setAdditionalData] = useState<AdditionalData>();
    const [refreshCounter, setRefreshCounter] = useState(0);
    const propsRef = useRef(props);

    useEffect(() => {
        propsRef.current = props;
    }, [props]);

    const refresh = useCallback(() => {
        setRefreshCounter(currentCounter => currentCounter + 1);
    }, []);

    useEffect(() => {
        if (props.controlRef == null) {
            return;
        }

        props.controlRef.current = {
            refresh: refresh,
        };
    }, [props.controlRef, refresh]);

    const resolvedPathParams = useMemo(() => ({
        ...params,
        [ID_PARAM]: id as string | undefined,
    }), [params, ID_PARAM, id]);

    const resolvedTabs = useMemo(() => {
        if (typeof props.tabs === 'function') {
            return props.tabs(item, isNewItem);
        }
        return props.tabs;
    }, [props.tabs, item, isNewItem]);

    const currentTab: number = useMemo(() => {
        return resolvedTabs
            .map(tab => generatePath(tab.path, resolvedPathParams))
            .findIndex(path => matchPath(location.pathname, path) != null);
    }, [location, resolvedPathParams, resolvedTabs]);

    const resolvedHeader = useMemo(() => {
        if (typeof props.header === 'function') {
            return props.header(item, isNewItem, notFound);
        }
        return props.header;
    }, [props.header, item, isNewItem, notFound]);

    useEffect(() => {
        const currentProps = propsRef.current;

        if (id == null) {
            setItem(undefined);
            setAdditionalData(undefined);
            setNotFound(false);
            setLoadError(undefined);
            if (currentProps.itemRef != null) {
                currentProps.itemRef.current = null;
            }
            if (currentProps.onItemChange != null) {
                currentProps.onItemChange(null);
            }
            if (currentProps.additionalDataRef != null) {
                currentProps.additionalDataRef.current = null;
            }
            if (currentProps.onAdditionalDataChange != null) {
                currentProps.onAdditionalDataChange(null);
            }
            return;
        }

        let isActive = true;
        setIsBusy(true);
        setLoadError(undefined);
        fetchData<ItemType, ID, AdditionalData>(api, id, currentProps)
            .then(({item, additionalData}) => {
                if (!isActive) {
                    return;
                }
                setItem(item);
                setAdditionalData(additionalData);
                setNotFound(false);
                setLoadError(undefined);
                if (currentProps.itemRef != null) {
                    currentProps.itemRef.current = item;
                }
                if (currentProps.onItemChange != null) {
                    currentProps.onItemChange(item ?? null);
                }
                if (currentProps.additionalDataRef != null) {
                    currentProps.additionalDataRef.current = additionalData;
                }
                if (currentProps.onAdditionalDataChange != null) {
                    currentProps.onAdditionalDataChange(additionalData ?? null);
                }
            })
            .catch((error: unknown) => {
                if (!isActive) {
                    return;
                }
                console.error(error);

                if (isApiError(error) && error.status === 404) {
                    setNotFound(true);
                    setLoadError(undefined);
                    return;
                }

                setNotFound(false);
                setLoadError(isApiError(error)
                    ? error
                    : {
                        status: 500,
                        message: 'Unexpected error while loading details.',
                        details: error,
                        displayableToUser: false,
                    });
            })
            .finally(() => {
                if (!isActive) {
                    return;
                }
                setIsBusy(false);
            });

        return () => {
            isActive = false;
        };
    }, [api, id, refreshCounter]);

    const headerTitle = useMemo(() => {
        if (props.getHeaderTitle) {
            return props.getHeaderTitle(item, isNewItem, notFound);
        }
        return resolvedHeader.title ?? 'Resource bearbeiten';
    }, [props.getHeaderTitle, item, isNewItem, notFound, resolvedHeader]);

    useEffect(() => {
        if (isNewItem || notFound || item == null) {
            return;
        }
        if (entityType == null) {
            return;
        }

        const searchItemId = propsRef.current.getSearchItemId?.(item, id) ?? String(id);
        if (searchItemId.length === 0) {
            return;
        }

        new SearchItemService()
            .recordRecentSearchItem({
                id: searchItemId,
                originTable: entityType,
            })
            .catch(() => {
            });
    }, [id, entityType, item, notFound]);

    if (loadError != null) {
        throw loadError;
    }

    if (!notFound) {
        // Custom checks take precedence for detail pages whose access rules cannot be expressed by
        // the standard create/read/update permission contract.
        if (props.hasAccess != null) {
            props.hasAccess(item);
        } else {
            ensureConfiguredAccess(item, isNewItem, permissionSet, permissionCheck);
        }

        const currentTabState = currentTab >= 0
            ? resolveTabState(resolvedTabs[currentTab], item, isNewItem, permissionSet, permissionCheck?.scope)
            : undefined;

        if (item != null && currentTabState?.missingPermission != null) {
            throw createPermissionDeniedError(currentTabState.missingPermission);
        }
    }

    const resolvedIsEditable = isEditable != null
        ? isEditable(item)
        : checkConfiguredEditability(item, isNewItem, permissionSet, permissionCheck);

    return (
        <>
            <Container>
                <GenericPageHeader {...resolvedHeader} title={isBusy ? "Wird geladen…" : headerTitle} isBusy={isBusy} />

                <Paper
                    sx={{
                        marginTop: 2.75,
                    }}
                >
                    {
                        resolvedTabs.length > 1 && !notFound &&
                            <Box
                                sx={{
                                    borderBottom: 1,
                                    borderBottomColor: 'divider',
                                }}
                            >

                                        <Tabs
                                            sx={{
                                                flex: 1,
                                            }}
                                            value={currentTab}
                                            onChange={(_, index: number) => {
                                                const tab = resolvedTabs[index];
                                                if (resolveTabState(tab, item, isNewItem, permissionSet, permissionCheck?.scope).disabled) {
                                                    return;
                                                }
                                                navigate(generatePath(tab.path, resolvedPathParams));
                                            }}
                                        >
                                            {
                                                resolvedTabs.length > 1 &&
                                                resolvedTabs.map((tab, index) => {
                                                    const tabState = resolveTabState(tab, item, isNewItem, permissionSet, permissionCheck?.scope);

                                                    return (
                                                        <Tab
                                                            key={tab.path}
                                                            value={index}
                                                            label={(
                                                                <DisabledTabLabel
                                                                    disabled={tabState.disabled}
                                                                    tooltip={tabState.tooltip}
                                                                >
                                                                    {tab.label}
                                                                </DisabledTabLabel>
                                                            )}
                                                            aria-disabled={tabState.disabled}
                                                            tabIndex={tabState.disabled ? -1 : undefined}
                                                            sx={tabState.disabled ? {
                                                                color: 'text.disabled',
                                                                cursor: 'not-allowed',
                                                                '&:hover': {
                                                                    color: 'text.disabled',
                                                                },
                                                            } : undefined}
                                                        />
                                                    );
                                                })
                                            }
                                        </Tabs>

                            </Box>
                    }
                    <Box
                        sx={{
                            padding: 2,
                        }}
                    >
                        {
                            notFound ?
                                <Stack
                                    direction="column"
                                    sx={{
                                        gap: 2,
                                        maxWidth: 440,
                                        margin: "40px auto 60px auto",
                                        textAlign: "center",
                                        alignItems: "center"
                                    }}>
                                    <NotFoundIllustration/>
                                    <Typography variant={"h2"} sx={{marginTop: 1}}>Diese Ressource konnte leider nicht (mehr) gefunden werden</Typography>
                                    <Typography>
                                        Die angeforderte Ressource existiert nicht oder wurde möglicherweise entfernt.
                                        Bitte überprüfen Sie die URL oder nutzen Sie eine der folgenden Möglichkeiten.
                                    </Typography>
                                    <Stack
                                        direction={"row"}
                                        sx={{
                                            gap: 2,
                                            marginTop: 1.5
                                        }}>
                                        {
                                            props.parentLink &&
                                            <Button
                                                variant={"contained"}
                                                size={"small"}
                                                startIcon={<FormatListBulletedOutlinedIcon />}
                                                component={Link}
                                                to={props.parentLink.to}
                                            >
                                                {props.parentLink.label}
                                            </Button>
                                        }
                                        <Button
                                            variant={"outlined"}
                                            size={"small"}
                                            startIcon={<ArrowBackOutlinedIcon />}
                                            onClick={() => {
                                                if (window.history.length > 2) {
                                                    navigate(-1);
                                                } else {
                                                    navigate('/');
                                                }
                                            }}
                                        >
                                            Zurück
                                        </Button>
                                    </Stack>
                                </Stack> :
                                <GenericDetailsPageContext.Provider
                                    value={{
                                        item: item,
                                        setItem: setItem,
                                        isNewItem: isNewItem,
                                        isExistingItem: !isNewItem,
                                        additionalData: additionalData,
                                        setAdditionalData: setAdditionalData,
                                        isBusy: isBusy,
                                        setIsBusy: setIsBusy,
                                        refresh: refresh,
                                        isEditable: resolvedIsEditable,
                                    }}
                                >
                                    <Outlet />
                                </GenericDetailsPageContext.Provider>
                        }
                    </Box>
                </Paper>
            </Container>
        </>
    );
}

function DisabledTabLabel(props: {
    disabled: boolean;
    tooltip: ReactNode;
    children: ReactNode;
}): ReactNode {
    return (
        <DisabledTooltip
            disabled={props.disabled}
            title={props.tooltip}
        >
            {props.children}
        </DisabledTooltip>
    );
}
