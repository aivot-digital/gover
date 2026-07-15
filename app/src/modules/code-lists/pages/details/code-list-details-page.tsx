import React, {ReactNode, useRef, useState} from 'react';
import {Typography} from '@mui/material';
import Sync from '@aivot/mui-material-symbols-400-outlined/dist/sync/Sync';
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
import SyncProblem from '@aivot/mui-material-symbols-400-outlined/dist/sync-problem/SyncProblem';

export function CodeListDetailsPage(): ReactNode {
    const dispatch = useAppDispatch();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });
    const detailsPageControlRef = useRef<GenericDetailsPageControlRef | null>(null);
    const [isSyncing, setIsSyncing] = useState(false);

    const handleSync = async (codeList: CodeList | undefined, keepOutdated: boolean): Promise<void> => {
        if (codeList == null || codeList.id === 0 || !isCodeListSyncable(codeList.sourceType) || isSyncing) {
            return;
        }

        setIsSyncing(true);
        try {
            await new CodeListsApiService().triggerUpdate(codeList.id, keepOutdated);
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
            title="Code-Liste bearbeiten"
            fullWidth
            background
        >
            <GenericDetailsPage<CodeList, number, void>
                header={(item) => ({
                    icon: ModuleIcons.codeLists,
                    title: 'Code-Liste bearbeiten',
                    badge: item != null && item.id !== 0
                        ? (
                            <CodeListStatusChip
                                status={item.status}
                                statusMessage={item.statusMessage}
                                lastSync={item.lastSync}
                            />
                        )
                        : undefined,
                    actions: [
                        {
                            tooltip: 'Code-Liste synchronisieren (veraltete Einträge behalten)',
                            icon: <Sync />,
                            onClick: () => {
                                void handleSync(item, true);
                            },
                            disabled: item == null || item.id === 0 || !isCodeListSyncable(item.sourceType) || isSyncing || !hasAccess,
                        },
                        {
                            tooltip: 'Code-Liste synchronisieren (veraltete Einträge entfernen)',
                            icon: <SyncProblem />,
                            onClick: () => {
                                void handleSync(item, false);
                            },
                            disabled: item == null || item.id === 0 || !isCodeListSyncable(item.sourceType) || isSyncing || !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Code-Listen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Code-Listen stellen zentrale Werte für Auswahlfelder bereit.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Einträge manueller Listen können im Tab Einträge gepflegt werden.
                                    Synchronisierte Listen beziehen ihre Einträge aus XRepository oder einer CSV-Datei.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Alle Code-Listen sind öffentlich verfügbar und dürfen keine vertraulichen Informationen enthalten.
                                </Typography>
                            </>
                        ),
                    },
                })}
                tabs={[
                    {
                        path: '/code-lists/:id',
                        label: 'Allgemeine Angaben',
                    },
                    {
                        path: '/code-lists/:id/items',
                        label: 'Einträge',
                        isDisabled: (item) => item?.id === 0,
                    },
                ]}
                initializeItem={() => new CodeListsApiService().initialize()}
                fetchData={(_, id: number) => new CodeListsApiService().retrieve(id)}
                getTabTitle={(item) => item.id === 0 ? 'Neue Code-Liste' : item.name}
                getHeaderTitle={(item, isNewItem, notFound) => {
                    if (notFound ?? false) return 'Code-Liste nicht gefunden';
                    if (isNewItem ?? false) return 'Neue Code-Liste anlegen';
                    return `Code-Liste: ${item?.name ?? 'Unbenannt'}`;
                }}
                parentLink={{
                    label: 'Liste der Code-Listen',
                    to: '/code-lists',
                }}
                entityType={ServerEntityType.CodeLists}
                isEditable={() => hasAccess}
                controlRef={detailsPageControlRef}
            />
        </PageWrapper>
    );
}
