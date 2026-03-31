import React from 'react';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {Typography} from '@mui/material';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {
    type ExtensionsDetailsPageItem,
    loadExtensionsDetailsPageItem,
} from '../../../../pages/staff-pages/settings/components/extensions/extensions';

export function ExtensionsPage() {
    return (
        <PageWrapper
            title="Erweiterungen"
            fullWidth
            background
        >
            <GenericDetailsPage<ExtensionsDetailsPageItem, string, undefined>
                header={{
                    icon: ModuleIcons.extensions,
                    title: 'Erweiterungen',
                    helpDialog: {
                        title: 'Hilfe zu Erweiterungen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Auf dieser Seite finden Sie alle auf Ihrer Gover-Instanz installierten Erweiterungen.
                                    Erweiterungen stellen zusätzliche Komponenten wie Prozessknoten, Operatoren,
                                    Speicheranbieter oder Integrationen bereit.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Die Übersicht fasst den aktuellen Umfang der installierten Erweiterungen zusammen.
                                    Im Reiter „Liste der Erweiterungen“ können Sie die einzelnen Plugins, deren Metadaten,
                                    Versionen, Changelogs und enthaltene Komponenten im Detail prüfen.
                                </Typography>
                            </>
                        ),
                    },
                }}
                tabs={[
                    {
                        path: '/settings/extensions',
                        label: 'Übersicht',
                    },
                    {
                        path: '/settings/extensions/list',
                        label: 'Liste der Erweiterungen',
                    },
                ]}
                initializeItem={() => ({
                    plugins: [],
                    loadingFailed: false,
                })}
                fetchData={async () => {
                    return loadExtensionsDetailsPageItem();
                }}
                getTabTitle={() => 'Erweiterungen'}
                getHeaderTitle={() => 'Erweiterungen'}
                idParam="*"
            />
        </PageWrapper>
    );
}
