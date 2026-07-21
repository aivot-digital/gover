import {type GridValidRowModel} from '@mui/x-data-grid';
import {Container, Paper} from '@mui/material';
import React, {type ReactNode, useCallback, useMemo, useState} from 'react';
import {GenericPageHeader} from '../generic-page-header/generic-page-header';
import {type GenericListColDef, type GenericListProps} from '../generic-list/generic-list-props';
import {type GenericPageHeaderProps} from '../generic-page-header/generic-page-header-props';
import {GenericList} from '../generic-list/generic-list';
import {type GenericListRowModel} from '../generic-list/generic-list-row-models';
import {type Action} from '../actions/actions-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectPermissions} from '../../slices/user-slice';
import {
    createPermissionDeniedError,
    formatMissingPermissionTooltip,
    hasScopedPermission,
    hasSystemPermission,
    type PermissionLike,
    type PermissionRequirement,
    type PermissionScope,
    resolvePermissionRequirement,
} from '../../modules/permissions/utils/permission-utils';
import {type PermissionSet} from '../../modules/permissions/models/permission-set';

export type GenericListPagePermissionScope<ItemType> = PermissionScope<ItemType>;

export type GenericListPagePermissionConfig<ItemType> = {
    scope?: GenericListPagePermissionScope<ItemType>;
    listAccess?: PermissionRequirement<ItemType> | Array<PermissionRequirement<ItemType>>;
    create?: PermissionLike;
    read?: PermissionLike;
    update?: PermissionLike;
};

export type GenericListPagePermissionState<ItemType> = {
    canCreate: boolean;
    createDisabledTooltip?: string;
    canRead: (item: ItemType) => boolean;
    canUpdate: (item: ItemType) => boolean;
    hasPermission: (permission: PermissionLike, item?: ItemType, scope?: PermissionScope<ItemType>) => boolean;
    getMissingPermissionTooltip: (permission: PermissionLike) => string;
};

type GenericListPageHeaderConfig<ItemType> =
    GenericPageHeaderProps
    | ((permissions: GenericListPagePermissionState<ItemType>) => GenericPageHeaderProps);

type GenericListPageColumnDefinitionsConfig<ItemType extends GridValidRowModel> =
    Array<GenericListColDef<ItemType> & { onlyFullScreen?: boolean; }>
    | ((permissions: GenericListPagePermissionState<ItemType>) => Array<GenericListColDef<ItemType> & { onlyFullScreen?: boolean; }>);

interface GenericListPageProps<ItemType extends GridValidRowModel> extends Omit<GenericListProps<ItemType>, 'columnDefinitions' | 'noDataPlaceholder' | 'rowActions'> {
    header: GenericListPageHeaderConfig<ItemType>;
    columnDefinitions: GenericListPageColumnDefinitionsConfig<ItemType>;
    noDataPlaceholder?: ReactNode | ((permissions: GenericListPagePermissionState<ItemType>) => ReactNode);
    rowActions?: (item: ItemType, permissions: GenericListPagePermissionState<ItemType>) => Action[];
    permissionCheck?: GenericListPagePermissionConfig<ItemType>;
}

export function GenericListPage<ItemType extends GenericListRowModel>(props: GenericListPageProps<ItemType>) {
    const {
        header,
        columnDefinitions,
        noDataPlaceholder,
        permissionCheck,
        rowActions,
        ...listProps
    } = props;
    const [isFullWidth, setIsFullWidth] = useState(false);
    const [isBusy, setIsBusy] = useState(false);
    const permissionSet = useAppSelector(selectPermissions);

    const permissionState = useMemo(() => createPermissionState(permissionSet, permissionCheck), [permissionSet, permissionCheck]);
    const missingListPermission = useMemo(() => findMissingListPermission(permissionSet, permissionCheck), [permissionSet, permissionCheck]);

    const resolvedHeader = useMemo(() => (
        typeof header === 'function'
            ? header(permissionState)
            : header
    ), [header, permissionState]);

    const resolvedColumnDefinitions = useMemo(() => (
        typeof columnDefinitions === 'function'
            ? columnDefinitions(permissionState)
            : columnDefinitions
    ), [columnDefinitions, permissionState]);

    const resolvedNoDataPlaceholder = useMemo(() => (
        typeof noDataPlaceholder === 'function'
            ? noDataPlaceholder(permissionState)
            : noDataPlaceholder
    ), [noDataPlaceholder, permissionState]);

    const resolvedRowActions = useCallback((item: ItemType) => {
        return rowActions?.(item, permissionState) ?? [];
    }, [rowActions, permissionState]);

    if (missingListPermission != null) {
        throw createPermissionDeniedError(missingListPermission);
    }

    return (
        <Container maxWidth={isFullWidth ? false : 'lg'}>
            <GenericPageHeader {...resolvedHeader} isBusy={isBusy} />

            <Paper
                sx={{
                    marginTop: 3.5,
                }}
            >
                <GenericList
                    {...listProps}
                    columnDefinitions={resolvedColumnDefinitions}
                    noDataPlaceholder={resolvedNoDataPlaceholder}
                    rowActions={rowActions == null ? undefined : resolvedRowActions}
                    onFullWidthChange={setIsFullWidth}
                    onBusyChange={setIsBusy}
                />
            </Paper>
        </Container>
    );
}

function createPermissionState<ItemType>(
    permissionSet: PermissionSet | undefined,
    permissionConfig: GenericListPagePermissionConfig<ItemType> | undefined,
): GenericListPagePermissionState<ItemType> {
    const scope = permissionConfig?.scope ?? {type: 'system' as const};
    const canCreate = permissionConfig?.create == null || hasSystemPermission(permissionSet, permissionConfig.create);

    return {
        canCreate: canCreate,
        createDisabledTooltip: permissionConfig?.create != null && !canCreate
            ? formatMissingPermissionTooltip(permissionConfig.create)
            : undefined,
        canRead: (item) => permissionConfig?.read == null || hasScopedPermission(permissionSet, item, scope, permissionConfig.read),
        canUpdate: (item) => permissionConfig?.update == null || hasScopedPermission(permissionSet, item, scope, permissionConfig.update),
        hasPermission: (permission, item, customScope) => hasScopedPermission(permissionSet, item, customScope ?? scope, permission),
        getMissingPermissionTooltip: formatMissingPermissionTooltip,
    };
}

function findMissingListPermission<ItemType>(
    permissionSet: PermissionSet | undefined,
    permissionConfig: GenericListPagePermissionConfig<ItemType> | undefined,
): PermissionLike | undefined {
    if (permissionConfig == null) {
        return undefined;
    }

    for (const requirement of getListAccessRequirements(permissionConfig)) {
        const resolvedRequirement = resolvePermissionRequirement(
            requirement,
            permissionConfig.scope ?? {type: 'system'},
        );

        if (resolvedRequirement != null && !hasScopedPermission(permissionSet, undefined, resolvedRequirement.scope, resolvedRequirement.permission)) {
            return resolvedRequirement.permission;
        }
    }

    return undefined;
}

function getListAccessRequirements<ItemType>(
    permissionConfig: GenericListPagePermissionConfig<ItemType>,
): Array<PermissionRequirement<ItemType>> {
    if (permissionConfig.listAccess != null) {
        return Array.isArray(permissionConfig.listAccess)
            ? permissionConfig.listAccess
            : [permissionConfig.listAccess];
    }

    // For system-scoped lists the read permission is enough to guard the route. Resource-scoped
    // lists need an explicit listAccess rule, because there is no row item while entering the page.
    if (permissionConfig.read != null && (permissionConfig.scope == null || permissionConfig.scope.type === 'system')) {
        return [permissionConfig.read];
    }

    return [];
}
