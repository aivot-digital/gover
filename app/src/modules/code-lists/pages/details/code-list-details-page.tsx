import React, {ReactNode, useRef, useState} from 'react';
import {Typography} from '@mui/material';
import Sync from '@aivot/mui-material-symbols-400-n25-outlined/Sync';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import {GenericDetailsPageControlRef} from '../../../../components/generic-details-page/generic-details-page-props';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeList} from '../../models/code-list';
import {CodeListStatusChip} from '../../components/code-list-status-chip';
import {isCodeListSyncable} from '../../enums/code-list-source-type';
import SyncProblem from '@aivot/mui-material-symbols-400-n25-outlined/SyncProblem';

export function CodeListDetailsPage(): ReactNode {
    const dispatch = useAppDispatch();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });
    const detailsPageControlRef = useRef<GenericDetailsPageControlRef | null>(null);
    const [isSyncing, setIsSyncing] = useState(false);

    const handleSync = async (codeList: CodeList | undefined, keepOutdated: boolean): Promise<void> => {
        if (codeList == null || codeList.key.length === 0 || !isCodeListSyncable(codeList.sourceType) || isSyncing) {
            return;
        }

        setIsSyncing(true);
        try {
            await new CodeListsApiService().triggerUpdate(codeList.key, keepOutdated);
            dispatch(showSuccessSnackbar('Die Synchronisierung wurde gestartet.'));
            detailsPageControlRef.current?.refresh();
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Starten der Synchronisierung ist ein Fehler aufgetreten.'));
        } finally {
            setIsSyncing(false);
        }
    };

    return (
        <PageWrapper
            title="Codeliste bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<CodeList, string, void>
                header={(item) => ({
                    icon: ModuleIcons.codeLists,
                    title: 'Codeliste bearbeiten',
                    badge: item != null && item.key.length > 0
                        ? (
                            <CodeListStatusChip
                                status={item.status}
                                sourceType={item.sourceType}
                                statusMessage={item.statusMessage}
                                lastSync={item.lastSync}
                            />
                        )
                        : undefined,
                    actions: item != null && item.key.length > 0 && isCodeListSyncable(item.sourceType) ? [
                        {
                            tooltip: 'Codeliste synchronisieren (veraltete Einträge behalten)',
                            icon: <Sync />,
                            onClick: () => {
                                void handleSync(item, true);
                            },
                            disabled: isSyncing || !hasAccess,
                        },
                        {
                            tooltip: 'Codeliste synchronisieren (veraltete Einträge entfernen)',
                            icon: <SyncProblem />,
                            onClick: () => {
                                void handleSync(item, false);
                            },
                            disabled: isSyncing || !hasAccess,
                        },
                    ] : [],
                    helpDialog: {
                        title: 'Hilfe zu Codelisten',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Codelisten bündeln wiederverwendbare Auswahlwerte für Formulare, Prozesse und Schnittstellen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Jede Codeliste legt fest, welche Beschriftung Benutzer:innen angezeigt wird und welcher technische Wert gespeichert oder weitergegeben wird.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Einträge können manuell gepflegt oder aus XRepository bzw. einer CSV-Datei synchronisiert werden.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Da Codelisten in öffentlichen Formularen genutzt und über die öffentliche Codelisten-API ohne Anmeldung abgerufen werden können,
                                    dürfen sie keine vertraulichen Informationen enthalten.
                                </Typography>
                            </>
                        ),
                    },
                })}
                tabs={[
                    {
                        path: '/code-lists/:key',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/code-lists/:key/items',
                        label: 'Einträge',
                        isDisabled: (item) => item?.key.length === 0,
                    },
                ]}
                initializeItem={() => new CodeListsApiService().initialize()}
                fetchData={(_, key: string) => new CodeListsApiService().retrieve(key)}
                getTabTitle={(item) => item.key.length === 0 ? 'Neue Codeliste' : item.name}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound ?? false) return 'Codeliste nicht gefunden';
                    if (isNewItem ?? false) return 'Neue Codeliste anlegen';
                    return `Codeliste: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Codelisten',
                    to: '/code-lists',
                }}
                entityType={ServerEntityType.CodeLists}
                isEditable={() => hasAccess}
                controlRef={detailsPageControlRef}
                idParam="key"
            />
        </PageWrapper>
    );
}
