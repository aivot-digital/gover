import React, { type PropsWithChildren, useState } from 'react';
import {type TableWrapperProps} from './table-wrapper-props';
import {DataGrid, type GridValidRowModel} from '@mui/x-data-grid';
import {ListHeader} from '../list-header/list-header';
import {Box} from '@mui/material';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export function TableWrapper<T extends GridValidRowModel>(props: PropsWithChildren<TableWrapperProps<T>>) {
    const {
        columns,
        rows,
        onRowClick,
        getRowId,
        noDataMessage,
        noDataFoundMessage,

        children,

        search,
        searchPlaceholder,
        onSearchChange,
        actions,
        hint,
        smallTitle,
        initialState,

        ...pageWrapperProps
    } = props;

    const [paginationModel, setPaginationModel] = useState({
        pageSize: 48,
        page: 0,
    });

    return (
        <>
            <ListHeader
                title={pageWrapperProps.title}
                search={search}
                searchPlaceholder={searchPlaceholder}
                onSearchChange={onSearchChange}
                actions={actions}
                hint={hint}
                smallTitle={smallTitle}
            />

            {
                children != null &&
                <Box>
                    {children}
                </Box>
            }

            <Box
                sx={{
                    width: '100%',
                    mt: 4,
                }}
            >
                <DataGrid
                    rows={rows}
                    columns={columns}
                    paginationModel={paginationModel}
                    onPaginationModelChange={(model) => setPaginationModel(model)}
                    pageSizeOptions={[12, 24, 48, 96]}
                    onRowClick={(event) => {
                        onRowClick(event.row);
                    }}
                    isRowSelectable={() => false}
                    disableColumnFilter={false}
                    disableColumnMenu={false}
                    autoHeight={true}
                    slots={{
                        noRowsOverlay: () => (
                            <Box
                                sx={{
                                    width: '100%',
                                    height: '100%',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center',
                                }}
                            >
                                {
                                    isStringNotNullOrEmpty(search) ?
                                        (noDataFoundMessage ?? 'Ihre Suche ergab keine Treffer') :
                                        (noDataMessage ?? 'Keine Einträge vorhanden')
                                }
                            </Box>
                        ),
                    }}
                    getRowId={getRowId}
                    sx={{
                        '& .MuiDataGrid-menuIcon button': {
                            zIndex: 999,
                            mr: 1,
                        },
                    }}
                    initialState={initialState}
                />
            </Box>
        </>
    );
}
