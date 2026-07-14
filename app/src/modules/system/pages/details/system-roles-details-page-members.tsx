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
import {useCheckSystemPermission} from '../../../permissions/hooks/use-permissions';
import {type Page} from '../../../../models/dtos/page';

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
    const {
        item: systemRole,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<SystemRoleEntity, undefined>;
    const canReadUsers = useCheckSystemPermission(Permission.USER_READ);

    if (systemRole == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

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
                        if (!canReadUsers) {
                            const page: Page<User> = {
                                content: [],
                                page: {
                                    number: 0,
                                    size: options.size,
                                    totalElements: 0,
                                    totalPages: 0,
                                },
                            };
                            return Promise.resolve(page);
                        }

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
                            title={canReadUsers ? 'Keine Mitarbeiter:innen zugeordnet' : 'Keine Mitarbeiter:innen sichtbar'}
                            description={canReadUsers
                                ? 'Diese Zuordnung vergibt globale Berechtigungen an Mitarbeiter:innen, unabhängig von Team oder Organisationseinheit.'
                                : `Für die Anzeige zugeordneter Mitarbeiter:innen ist die Berechtigung ${Permission.USER_READ} erforderlich.`}
                        />
                    }
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
