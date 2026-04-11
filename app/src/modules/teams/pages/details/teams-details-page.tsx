import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {GenericPageHeaderPropsHelpDialog} from '../../../../components/generic-page-header/generic-page-header-props';
import {TeamsApiService} from '../../services/teams-api-service';
import {TeamEntity} from "../../entities/team-entity";
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";

export function TeamsDetailsPage() {
    return (
        <PageWrapper
            title="Team bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<TeamEntity, number, void>
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
                        isDisabled: (item) => !item?.id,
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
                entityType={ServerEntityType.Departments}
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
