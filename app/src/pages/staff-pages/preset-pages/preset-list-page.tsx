import {PresetsService} from '../../../services/presets.service';
import {Preset} from '../../../models/entities/preset';
import {ElementType} from '../../../data/element-type/element-type';
import {generateElementIdForType} from "../../../utils/id-utils";
import ProjectPackage from '../../../../package.json';
import React, {useEffect, useState} from "react";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";
import {GridColDef} from "@mui/x-data-grid";
import {useNavigate} from "react-router-dom";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";


const columns: GridColDef<Preset>[] = [
    {
        field: 'title',
        headerName: 'Titel',
        valueGetter: params => params.row.root.name,
        flex: 1,
    },
];

export function PresetListPage() {
    const navigate = useNavigate();
    const [presets, setPresets] = useState<Preset[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        PresetsService
            .list()
            .then(setPresets);
    }, []);

    const navigateTo = (preset: Preset) => {
        navigate(`/presets/edit/${preset.id}`);
    }

    const handleCreate = () => {
        const id = generateElementIdForType(ElementType.Container);
        return PresetsService
            .create({
                id: 0,
                root: {
                    id: id,
                    type: ElementType.Container,
                    appVersion: ProjectPackage.version,
                    name: `Neue Vorlage ${(presets?.length ?? 0) + 1}`,
                    children: [],
                },
                created: '',
                updated: '',
            })
            .then(navigateTo);
    };

    const filtered = presets != null ? presets.filter(dest => dest.root.name?.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title="Vorlagen"
            isLoading={presets == null}
            actions={[{
                label: 'Neue Vorlage',
                onClick: handleCreate,
                icon: <AddOutlinedIcon/>,
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
