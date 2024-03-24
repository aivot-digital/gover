import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {type GridColDef} from '@mui/x-data-grid';
import {TablePageWrapper} from '../../../components/table-page-wrapper/table-page-wrapper';
import {type ProviderLink} from '../../../models/entities/provider-link';
import {filterItems} from '../../../utils/filter-items';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {format, parseISO} from 'date-fns';
import {useApi} from '../../../hooks/use-api';
import {useProviderLinksApi} from '../../../hooks/use-provider-links-api';

const columns: Array<GridColDef<ProviderLink>> = [
    {
        field: 'text',
        headerName: 'Text',
        flex: 1,
    },
    {
        field: 'link',
        headerName: 'Link',
        flex: 1,
    },
    {
        field: 'updated',
        headerName: 'Letzte Aktualisierung',
        flex: 1,
        type: 'date',
        valueFormatter: (params) => format(parseISO(params.value), 'dd.MM.yyyy HH:mm'),
    },
];

export function ProviderLinkListPage(): JSX.Element {
    useAdminGuard();

    const api = useApi();
    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [links, setLinks] = useState<ProviderLink[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        useProviderLinksApi(api)
            .listProviderLinks()
            .then(setLinks)
            .catch((err) => {
                console.error(err);
                setLoadError('Die Liste der Links konnte nicht geladen werden.');
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const filtered = filterItems(links, 'text', search);

    return (
        <TablePageWrapper
            title="Links"
            isLoading={isLoading}
            error={loadError}

            hint={{
                text: 'Hier können Sie Links anlegen, die dann auf der Startseite angezeigt werden.',
                moreLink: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/home' /* TODO: Link anpassen */,
            }}

            columns={columns}
            rows={filtered ?? []}
            onRowClick={(dest) => {
                navigate(`/provider-links/${dest.id}`);
            }}

            search={search}
            searchPlaceholder="Link suchen..."
            onSearchChange={setSearch}

            actions={[{
                label: 'Neuer Link',
                icon: <AddOutlinedIcon/>,
                tooltip: 'Neuen Link anlegen',
                link: '/provider-links/new',
            }]}
        />
    );
}
