import React, { type PropsWithChildren } from 'react';
import { type TablePageWrapperProps } from './table-page-wrapper-props';
import { PageWrapper } from '../page-wrapper/page-wrapper';
import { DataGrid, type GridValidRowModel } from '@mui/x-data-grid';
import { ListHeader } from '../list-header/list-header';
import { Box } from '@mui/material';
import { isStringNotNullOrEmpty } from '../../utils/string-utils';

export function TablePageWrapper<T extends GridValidRowModel>(props: PropsWithChildren<TablePageWrapperProps<T>>): JSX.Element {
    const {
        columns,
        rows,
        onRowClick,

        children,

        search,
        searchPlaceholder,
        onSearchChange,
        actions,

        ...pageWrapperProps
    } = props;

    return (
        <PageWrapper { ...pageWrapperProps }>
            <ListHeader
                title={ pageWrapperProps.title }
                search={ search }
                searchPlaceholder={ searchPlaceholder }
                onSearchChange={ onSearchChange }
                actions={ actions }
            />

            {
                children != null &&
                <Box>
                    { children }
                </Box>
            }

            <Box
                sx={ {
                    height: 'calc(100vh - 256px)',
                    width: '100%',
                    mt: 4,
                } }
            >
                <DataGrid
                    rows={ rows }
                    columns={ columns }
                    pageSize={ 20 }
                    rowsPerPageOptions={ [20] }
                    onRowClick={ (event) => {
                        onRowClick(event.row);
                    } }
                    disableSelectionOnClick={ true }
                    disableColumnFilter={ true }
                    disableColumnMenu={ true }
                    components={ {
                        NoRowsOverlay: () => (
                            <Box
                                sx={ {
                                    width: '100%',
                                    height: '100%',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                } }
                            >
                                {
                                    isStringNotNullOrEmpty(search) ?
                                        'Ihre Suche ergab keine Treffer' :
                                        'Keine Einträge vorhanden'
                                }
                            </Box>
                        ),
                    } }
                />
            </Box>
        </PageWrapper>
    );
}
