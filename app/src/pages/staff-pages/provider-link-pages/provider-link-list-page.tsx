import { useAuthGuard } from '../../../hooks/use-auth-guard';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { faPlus } from '@fortawesome/pro-light-svg-icons';
import { type GridColDef } from '@mui/x-data-grid';
import { useUserGuard } from '../../../hooks/use-user-guard';
import { TablePageWrapper } from '../../../components/table-page-wrapper/table-page-wrapper';
import { ProviderLinksService } from '../../../services/provider-links-service';
import { type ProviderLink } from '../../../models/entities/provider-link';
import { filterItems } from '../../../utils/filter-items';

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
];

export function ProviderLinkListPage(): JSX.Element {
    useAuthGuard();
    useUserGuard((user) => user?.admin ?? false);

    const navigate = useNavigate();

    const [search, setSearch] = useState('');
    const [links, setLinks] = useState<ProviderLink[]>();
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        setIsLoading(true);
        setLoadError(undefined);

        ProviderLinksService
            .list()
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
            isLoading={ isLoading }
            error={ loadError }

            columns={ columns }
            rows={ filtered ?? [] }
            onRowClick={ (dest) => {
                navigate(`/provider-links/${ dest.id }`);
            } }

            search={ search }
            searchPlaceholder="Link suchen..."
            onSearchChange={ setSearch }

            actions={ [{
                label: 'Neuer Link',
                icon: faPlus,
                tooltip: 'Neuen Link anlegen',
                link: '/provider-links/new',
            }] }
        />
    );
}
