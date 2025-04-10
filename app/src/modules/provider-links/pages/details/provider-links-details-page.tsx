import React from 'react';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Typography} from '@mui/material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import InsertLinkOutlinedIcon from '@mui/icons-material/InsertLinkOutlined';
import {ProviderLinksApiService} from '../../provider-links-api-service';
import {ProviderLink} from '../../models/provider-link';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';

export function ProviderLinksDetailsPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Link bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<ProviderLink, number, undefined>
                header={{
                    icon: <InsertLinkOutlinedIcon />,
                    title: 'Link bearbeiten',
                    helpDialog: {
                        title: 'Hilfe zu Links',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Hier können Sie Verlinkungen anlegen, welche anschließend auf der Startseite der Gover-Instanz für angemeldete Nutzer:innen angezeigt werden.
                                    Diese Funktion kann z. B. dafür genutzt werden, um auf externe Seiten oder interne Inhalte zu verweisen, die wichtig für Ihr Team sein könnten.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    paragraph
                                    sx={{mt: 2}}
                                >
                                    Ein Link besteht aus einem Linktext und einer URL. Der Linktext wird auf der Startseite angezeigt und die URL wird beim Klick auf den Link geöffnet.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/links/:id',
                        label: '',
                    },
                ]}
                initializeItem={(api) => new ProviderLinksApiService(api).initialize()}
                fetchData={(api, id: number) => new ProviderLinksApiService(api).retrieve(id)}
                getTabTitle={(item: ProviderLink) => {
                    if (item.id === 0) {
                        return 'Neuer Link';
                    } else {
                        return item.text;
                    }
                }}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound) return "Link nicht gefunden";
                    if (isNewItem) return "Neuen Link anlegen";
                    return `Link: ${item?.text ?? "Unbenannt"}`;
                }}
                parentLink={{
                    label: "Liste der Links",
                    to: "/provider-links",
                }}
            />
        </PageWrapper>
    );
}
