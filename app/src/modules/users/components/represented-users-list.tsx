import {useCallback} from 'react';
import {GenericList} from '../../../components/generic-list/generic-list';
import {type GenericListPropsFetchOptions} from '../../../components/generic-list/generic-list-props';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {type Page} from '../../../models/dtos/page';
import {type VUserDeputyWithDetailsEntity} from '../entities/v-user-deputy-with-details-entity';
import {VUserDeputyWithDetailsApiService} from '../services/v-user-deputy-with-details-api-service';
import {createDeputyColumns} from './deputy-columns';

const columns = createDeputyColumns('originalUserFullName', 'Vertretene Mitarbeiter:in');

interface RepresentedUsersListProps {
    deputyUserId: string;
    emptyDescription: string;
}

/**
 * Read-only list of the people a user represents or will represent. Users without the system permission to read
 * deputies only receive relations they are part of, which covers their own account.
 */
export function RepresentedUsersList({deputyUserId, emptyDescription}: RepresentedUsersListProps) {
    const fetchRelations = useCallback((options: GenericListPropsFetchOptions<VUserDeputyWithDetailsEntity>): Promise<Page<VUserDeputyWithDetailsEntity>> => {
        return new VUserDeputyWithDetailsApiService()
            .listAllOrdered(options.sort, options.order, {
                deputyUserId,
                originalUserFullName: options.search,
            });
    }, [deputyUserId]);

    return (
        <GenericList<VUserDeputyWithDetailsEntity>
            disableFullWidthToggle={true}
            sx={{
                mx: '-16px',
                mb: '-16px',
            }}
            columnDefinitions={columns}
            fetch={fetchRelations}
            getRowIdentifier={(item) => item.id.toString()}
            searchLabel="Vertretene Mitarbeiter:in suchen"
            searchPlaceholder="Name der vertretenen Mitarbeiter:in eingeben…"
            defaultSortField="untilDate"
            rowMenuItems={[]}
            noDataPlaceholder={
                <EmptyDataListPlaceholder
                    title="Keine Vertretungen eingetragen"
                    description={emptyDescription}
                />
            }
            loadingPlaceholder="Lade Vertretungen…"
            noSearchResultsPlaceholder="Keine vertretenen Mitarbeiter:innen gefunden"
        />
    );
}
