import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {DataObjectSchema} from '../../models/data-object-schema';
import {uploadObjectFile} from '../../../../utils/download-utils';
import {useNavigate} from 'react-router-dom';
import {v4 as uuid4} from 'uuid';
import CloudUploadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CloudUpload';
import FolderData from '@aivot/mui-material-symbols-400-n25-outlined/FolderData';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {useCheckSystemPermission, useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';

export function DataObjectSchemaListPage() {
    const navigate = useNavigate();
    useHasSystemPermission(Permission.OBJECT_SCHEMA_READ);
    const canCreateDataObjectSchema = useCheckSystemPermission(Permission.OBJECT_SCHEMA_CREATE);
    const canUpdateDataObjectSchema = useCheckSystemPermission(Permission.OBJECT_SCHEMA_UPDATE);

    const handleImport = useCallback(() => {
        uploadObjectFile<DataObjectSchema>('application/json')
            .then((importedSchema) => {
                if (importedSchema == null) {
                    return;
                }
                const importUUID = uuid4();
                sessionStorage.setItem(`import/${importUUID}`, JSON.stringify(importedSchema));
                navigate('/data-models/new', {
                    state: importedSchema,
                });
            });
    }, [navigate]);

    const header = useMemo(() => ({
        icon: <FolderData />,
        title: 'Datenmodelle',
        actions: [
            {
                icon: <CloudUploadOutlinedIcon />,
                onClick: handleImport,
                variant: 'text' as const,
                label: 'Importieren',
                disabled: !canCreateDataObjectSchema,
                disabledTooltip: formatMissingPermissionTooltip(Permission.OBJECT_SCHEMA_CREATE),
            },
            {
                label: 'Neues Datenmodell',
                icon: <AddOutlinedIcon />,
                to: '/data-models/new',
                variant: 'contained' as const,
                disabled: !canCreateDataObjectSchema,
                disabledTooltip: formatMissingPermissionTooltip(Permission.OBJECT_SCHEMA_CREATE),
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Datenmodellen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Ein Datenmodell beschreibt die Struktur eines Datenobjekts in Gover und legt fest, welche Datenfelder existieren, welche Datentypen sie haben, welche Standardwerte gelten und wie Werte geprüft werden.
                        Es sorgt dafür, dass Daten aus Formularen, Prozessen und Schnittstellen konsistent, valide und eindeutig interpretierbar sind.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Dazu können auch verschachtelte Objekte, Pflichtangaben, Wertebereiche oder Muster sowie Beschreibungen, Labels und optionale Sichtbarkeitsregeln gehören. Dasselbe Datenmodell kann in mehreren
                        Prozessen und Komponenten wiederverwendet werden, sodass überall dieselbe Definition gilt. Bei der Ausgestaltung empfiehlt es sich, sprechende und langlebige Feldnamen zu verwenden,
                        Weiterentwicklungen kompatibel vorzunehmen (zum Beispiel Felder hinzufügen statt umzubenennen oder zu entfernen) und Validierungen deutlich zu setzen.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Bei der Beziehung zwischen Datenmodell und Datenobjekt gilt: Das Datenmodell definiert die Form und das Datenobjekt füllt diese Form mit konkreten Werten. Änderungen am Datenmodell beeinflussen, wie
                        neue oder geänderte Datenobjekte geprüft und gespeichert werden.
                    </Typography>
                </>
            ),
        },
    }), [handleImport, canCreateDataObjectSchema]);

    const fetchSchemas = useCallback((options: GenericListPropsFetchOptions<DataObjectSchema>) => {
        return new DataObjectSchemasApiService(options.api)
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {
                    name: options.search,
                },
            );
    }, []);

    const columnDefinitions = useMemo(() => [
        {
            field: 'icon',
            headerName: '',
            renderCell: () => <CellContentWrapper><FolderData /></CellContentWrapper>,
            disableColumnMenu: true,
            width: 24,
            sortable: false,
        },
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/data-models/${params.row.key}`}
                    title={canUpdateDataObjectSchema ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
    ], [canUpdateDataObjectSchema]);

    const getRowIdentifier = useCallback((row: DataObjectSchema) => row.key.toString(), []);

    const rowActions = useCallback((item: DataObjectSchema) => [
        {
            icon: canUpdateDataObjectSchema ? <EditOutlined /> : <Visibility />,
            to: `/data-models/${item.key}`,
            tooltip: canUpdateDataObjectSchema ? 'Datenmodell bearbeiten' : 'Datenmodell anzeigen',
        },
        {
            icon: <DataObject />,
            to: `/data-objects/${item.key}`,
            tooltip: 'Datenobjekte zu diesem Modell anzeigen',
        },
    ], [canUpdateDataObjectSchema]);

    return (
        <>
            <PageWrapper
                title="Datenmodelle"
                fullWidth
                background
            >
                <GenericListPage<DataObjectSchema>
                    header={header}
                    searchLabel="Datenmodell suchen"
                    searchPlaceholder="Name des Datenmodells eingeben…"
                    fetch={fetchSchemas}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Noch keine Datenmodelle angelegt"
                            description="Datenmodelle definieren die Struktur wiederverwendbarer Datensätze, die in Formularen und Prozessen genutzt werden können."
                            addText="Neues Datenmodell anlegen"
                            onAdd={() => navigate('/data-models/new')}
                            addDisabled={!canCreateDataObjectSchema}
                            addDisabledTooltip={formatMissingPermissionTooltip(Permission.OBJECT_SCHEMA_CREATE)}
                        />
                    }
                    noSearchResultsPlaceholder="Keine Datenmodelle gefunden"
                    rowActionsCount={2}
                    rowActions={rowActions}
                    defaultSortField="name"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>
        </>
    );
}
