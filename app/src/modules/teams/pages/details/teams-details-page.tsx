import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {GenericPageHeaderPropsHelpDialog} from '../../../../components/generic-page-header/generic-page-header-props';
import {TeamsApiService} from '../../services/teams-api-service';
import {TeamEntity} from "../../entities/team-entity";
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {useCallback} from 'react';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {
    checkSystemPermission,
    checkTeamPermission,
    formatMissingPermissionTooltip,
    hasSystemPermission,
    hasTeamPermission,
} from '../../../permissions/utils/permission-utils';

export function TeamsDetailsPage() {
    const permissions = useAppSelector(selectPermissions);
    const isEditable = useCallback((item: TeamEntity | undefined) => {
        if (item == null) {
            return false;
        }

        if (item.id === 0) {
            return checkSystemPermission(permissions, Permission.TEAM_CREATE);
        }

        return checkTeamPermission(permissions, item.id, Permission.TEAM_UPDATE);
    }, [permissions]);
    const hasAccess = useCallback((item: TeamEntity | undefined) => {
        if (item == null) {
            return;
        }

        if (item.id === 0) {
            hasSystemPermission(permissions, Permission.TEAM_CREATE);
            return;
        }

        hasTeamPermission(permissions, item.id, Permission.TEAM_READ);
    }, [permissions]);

    return (
        <PageWrapper
            title="Team bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<TeamEntity, number, void>
                hasAccess={hasAccess}
                isEditable={isEditable}
                header={{
                    icon: ModuleIcons.teams,
                    title: 'Team bearbeiten',
                    helpDialog: HelpDialogContent,
                }}
                tabs={[
                    {
                        path: '/teams/:id',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/teams/:id/members',
                        label: 'Teammitglieder',
                        isDisabled: (item) => !item?.id || !checkTeamPermission(permissions, item.id, Permission.TEAM_MEMBERSHIP_READ),
                        disabledTooltip: (item) => item?.id
                            ? formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_READ)
                            : undefined,
                    },
                ]}
                initializeItem={(api) => TeamsApiService.initialize()}
                fetchData={(api, id: number) => new TeamsApiService().retrieve(id)}
                getTabTitle={(item) => {
                    if (item.id === 0 || item.name == null) {
                        return 'Neues Team';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound || item == null) {
                        return 'Team nicht gefunden';
                    }
                    if (isNewItem) {
                        return 'Neues Team anlegen';
                    }
                    return `Team: ${item.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Teams',
                    to: '/teams',
                }}
                entityType={ServerEntityType.Teams}
            />
        </PageWrapper>
    );
}

const HelpDialogContent: GenericPageHeaderPropsHelpDialog = {
    title: 'Hilfe zu Teams',
    tooltip: 'Hilfe anzeigen',
    content: (
        <>
            <Typography>
                Teams bündeln Mitarbeiter:innen für gemeinsame Aufgaben und Zuständigkeiten innerhalb
                von Gover.
            </Typography>
            <Typography sx={{mt: 2}}>
                Über Teammitgliedschaften und zugewiesene Domänenrollen steuern Sie, welche
                Mitarbeiter:innen innerhalb dieses Teams welche Berechtigungen erhalten.
            </Typography>
        </>
    ),
};
