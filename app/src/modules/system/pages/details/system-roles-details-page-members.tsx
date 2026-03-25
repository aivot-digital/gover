import React, {useContext} from 'react';
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
                    Eine Übersicht der Mitarbeiter:innen, die dieser Rolle zugeordnet sind.
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
                    noDataPlaceholder="Keine Mitarbeiter:innen vorhanden"
                    loadingPlaceholder="Lade Mitarbeiter:innen…"
                    noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                />
            </Box>
        </>
    );
}
