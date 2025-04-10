import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Destination} from '../../models/destination';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import {DestinationsApiService} from '../../destinations-api-service';
import React from 'react';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function DestinationDetailsPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Schnittstelle bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<Destination, number, undefined>
                header={{
                    icon: <DataObjectOutlinedIcon />,
                    title: 'Schnittstelle bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Schnittstellen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Schnittstellen dienen in der Gover-Anwendung zur Übertragung von durch Bürger:innen gestellten Anträgen in Folgesysteme.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Über eine E-Mail-Schnittstelle können Sie eingegangene Anträge an eine oder mehrere E-Mail-Adressen senden lassen.
                                    Über eine HTTP-Schnittstelle können Sie eingegangene Anträge an eine HTTP-Adresse via POST-Anfrage übertragen lassen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Falls einen von ihnen benötigte Schnittstelle nicht vorhanden ist, bietet Aivot Ihnen die Möglichkeit, neue Schnittstellen zum System hinzufügen zu lassen.
                                    So können Sie beispielsweise Ihre eigenen Fachverfahren oder Folgesysteme anschließen. Bitte wenden Sie sich dazu an den Support.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/destinations/:id',
                        label: 'Konfiguration',
                    },
                    {
                        path: '/destinations/:id/forms',
                        label: 'Formulare',
                        isDisabled: (item) => !item?.id,
                    },
                ]}
                initializeItem={(api) => new DestinationsApiService(api).initialize()}
                fetchData={(api, id: number) => new DestinationsApiService(api).retrieve(id)}
                getTabTitle={(item: Destination) => {
                    if (item.id === 0) {
                        return 'Neue Schnittstelle';
                    } else {
                        return item.name;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return "Schnittstelle nicht gefunden";
                    if (isNewItem) return "Neue Schnittstelle anlegen";
                    return `Schnittstelle: ${item?.name ?? "Unbenannt"}`;
                }}
                parentLink={{
                    label: "Liste der Schnittstellen",
                    to: "/destinations",
                }}
            />
        </PageWrapper>
    );
}