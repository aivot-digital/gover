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
                Ein Fachbereich ist eine zentrale Verwaltungseinheit in Gover und essenziell für den Betrieb der Anwendung. Er speichert wichtige Stammdaten wie Adress- und Kontaktdaten sowie rechtliche Informationen (z.
                B. Impressum, Datenschutzerklärung), die in Formularen wiederverwendet werden können.
            </Typography>
            <Typography sx={{mt: 2}}>
                Jedem Fachbereich sind Mitarbeiter:innen mit einer spezifischen Rolle zugeordnet, die deren Berechtigungen innerhalb des Fachbereichs definiert.
            </Typography>
        </>
    ),
};