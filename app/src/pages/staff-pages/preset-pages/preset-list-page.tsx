import React, { useEffect, useState } from 'react';
import { PresetsService } from '../../../services/presets.service';
import { type Preset } from '../../../models/entities/preset';
import { ElementType } from '../../../data/element-type/element-type';
import { generateElementIdForType } from '../../../utils/id-utils';
import ProjectPackage from '../../../../package.json';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { type GridColDef } from '@mui/x-data-grid';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { showErrorSnackbar } from '../../../slices/snackbar-slice';

const columns: Array<GridColDef<Preset>> = [
    {
        field: 'title',
        headerName: 'Titel',
        valueGetter: (params) => params.row.root.name,
        flex: 1,
    },
];

export function PresetListPage(): JSX.Element {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const [presets, setPresets] = useState<Preset[]>();
    const [search, setSearch] = useState('');

    const [isLoading, setIsLoading] = useState(false);
    const [loadingError, setLoadingError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadingError(undefined);

        PresetsService
            .list()
            .then(setPresets)
            .catch((err) => {
                console.error(err);
                setLoadingError('Die Liste der Vorlagen konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const navigateTo = (preset: Preset): void => {
        navigate(`/presets/edit/${ preset.id }`);
    };

    const handleCreate = (): void => {
        const id = generateElementIdForType(ElementType.Container);
        setIsLoading(true);
        PresetsService
            .create({
                id: 0,
                root: {
                    id,
                    type: ElementType.Container,
                    appVersion: ProjectPackage.version,
                    name: `Neue Vorlage ${ (presets?.length ?? 0) + 1 }`,
                    children: [],
                },
                created: '',
                updated: '',
            })
            .then(navigateTo)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Neue Vorlage konnte nicht angelegt werden.'));
                setIsLoading(false);
            });
    };

    const filtered = presets != null ? presets.filter((dest) => dest.root.name?.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Vorlagen"
            isLoading={isLoading}
            error={loadingError}
            actions={[{
                label: 'Neue Vorlage',
                onClick: handleCreate,
                icon: faPlus,
                tooltip: 'Neue Vorlage hinzufügen',
            }]}

            rows={filtered ?? []}
            columns={columns}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Vorlage suchen..."

            onRowClick={navigateTo}
        />
    );
}
