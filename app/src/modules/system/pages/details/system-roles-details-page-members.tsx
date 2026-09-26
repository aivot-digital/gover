import React, {useContext} from 'react';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {type GridColDef} from '@mui/x-data-grid';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import {CellLink} from "../../../../components/cell-link/cell-link";
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from "../../../../components/generic-details-page/generic-details-page-context";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import {GenericList} from "../../../../components/generic-list/generic-list";
import {User} from "../../../users/models/user";
import {UsersApiService} from "../../../users/users-api-service";
import {SystemRoleEntity} from "../../entities/system-role-entity";
import {Permission} from '../../../../data/permissions/permission';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {requireSystemPermission} from '../../../permissions/utils/permission-utils';

const columns: Array<GridColDef<User>> = [
    {
        field: 'fullName',
        headerName: 'Mitarbeiter:in',
        flex: 1,
        renderCell: (params) => (
            <CellLink
                to={`/users/${params.row.id}`}
                title="Mitarbeiter:in anzeigen"
            >
                {String(params.row.fullName)}
            </CellLink>
        ),
    },
];

export function SystemRolesDetailsPageMembers() {
    const permissions = useAppSelector(selectPermissions);
    const {
        item: systemRole,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<SystemRoleEntity, undefined>;

    if (systemRole == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    requireSystemPermission(permissions, Permission.USER_READ);

    return (
        <>
            <Box sx={{pt: 2}}>
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Zugeordnete Mitarbeiter:innen
                </Typography>

                <Typography sx={{
                    mb: 3,
                    maxWidth: 900,
                }}>
                    Eine Übersicht der Mitarbeiter:innen, die dieser Systemrolle zugeordnet sind.
                </Typography>

                <GenericList<User>
                    disableFullWidthToggle={true}
                    sx={{
                        mx: '-16px',
                        mb: '-16px',
                    }}
                    columnDefinitions={columns}
                    fetch={(options) => {
                        return new UsersApiService()
                            .list(options.page, options.size, options.sort, options.order, {
                                name: options.search,
                                systemRoleId: systemRole.id,
                            });
                    }}
                    getRowIdentifier={(item) => item.id.toString()}
                    searchLabel="Mitarbeiter:in suchen"
                    searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                    defaultSortField="fullName"
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Mitarbeiter:innen zugeordnet"
                            description="Diese Zuordnung vergibt globale Berechtigungen an Mitarbeiter:innen, unabhängig von Team oder Organisationseinheit."
                        />
                    }
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
