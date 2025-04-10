import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import PaletteOutlinedIcon from '@mui/icons-material/PaletteOutlined';
import {type Theme} from '../../models/theme';
import {ThemesApiService} from '../../themes-api-service';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function ThemeDetailsPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Farbschema bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Theme, number, undefined>
                header={{
                    icon: <PaletteOutlinedIcon />,
                    title: 'Farbschema bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Farbschemata',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Farbschema ist eine Sammlung von Farben, die in der Benutzeroberfläche von Gover verwendet werden. Farbschemata können global oder für einzelne Formulare genutzt werden.
                                    So können Sie z. B. für verschiedene Fachbereiche oder Abteilungen unterschiedliche Farbschemata anlegen und nutzen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Ein Farbschema besteht aus einem Namen und einer Liste von Farben. Bei der Auswahl der Farben sollte die Barrierfreiheit berücksichtigt werden.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/themes/:id',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/themes/:id/forms',
                        label: 'Formulare',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new ThemesApiService(api).initialize()}
                fetchData={(api, id: number) => new ThemesApiService(api).retrieve(id)}
                getTabTitle={(item: Theme) => {
                    if (item.id === 0) {
                        return 'Neues Farbschema';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return "Farbschema nicht gefunden";
                    if (isNewItem) return "Neues Farbschema anlegen";
                    return `Farbschema: ${item?.name ?? "Unbenannt"}`;
                }}
                parentLink={{
                    label: "Liste der Farbschemata",
                    to: "/themes",
                }}
            />
        </PageWrapper>
    );
}