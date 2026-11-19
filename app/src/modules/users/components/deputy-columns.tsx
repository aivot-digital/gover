import {type GridColDef} from '@mui/x-data-grid';
import {type VUserDeputyWithDetailsEntity} from '../entities/v-user-deputy-with-details-entity';
import {formatLocalDate} from '../../../utils/date-utils';

/** Columns of a deputy relation list, naming either the deputy or the represented person. */
export function createDeputyColumns(
    personField: 'deputyUserFullName' | 'originalUserFullName',
    personHeader: string,
): Array<GridColDef<VUserDeputyWithDetailsEntity>> {
    return [
        {
            field: personField,
            headerName: personHeader,
            flex: 1,
        },
        {
            field: 'fromDate',
            headerName: 'Ab',
            flex: 1,
            renderCell: (params) => (
                <span>
                    {formatLocalDate(params.row.fromDate)}
                </span>
            ),
        },
        {
            field: 'untilDate',
            headerName: 'Bis',
            flex: 1,
            renderCell: (params) => params.row.untilDate != null ? (
                <span>
                    {formatLocalDate(params.row.untilDate)}
                </span>
            ) : (
                <em>Unbegrenzt</em>
            ),
        },
        {
            field: 'active',
            headerName: 'Derzeit aktiv',
            flex: 1,
            type: 'boolean',
        },
    ];
}
