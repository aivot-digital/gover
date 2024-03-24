import React, {type PropsWithChildren} from 'react';
import {type TableWrapperProps} from './table-wrapper-props';
import {DataGrid, type GridValidRowModel} from '@mui/x-data-grid';
import {ListHeader} from '../list-header/list-header';
import {Box, IconButton} from '@mui/material';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export function TableWrapper<T extends GridValidRowModel>(props: PropsWithChildren<TableWrapperProps<T>>): JSX.Element {
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

        ...pageWrapperProps
    } = props;

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
                    height: 'calc(100vh - 256px)',
                    width: '100%',
                    mt: 4,
                }}
            >
                <DataGrid
                    rows={rows}
                    columns={columns}
                    pageSize={20}
                    rowsPerPageOptions={[20]}
                    onRowClick={(event) => {
                        onRowClick(event.row);
                    }}
                    disableSelectionOnClick={true}
                    disableColumnFilter={false}
                    disableColumnMenu={false}
                    components={{
                        NoRowsOverlay: () => (
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
                    componentsProps={{
                        baseFormControl: {
                            fullWidth: false,
                            margin: 'none',
                        },
                        baseTextField: {
                            fullWidth: false,
                            margin: 'none',
                        },
                    }}
                    getRowId={getRowId}
                    sx={{
                        '& .MuiDataGrid-menuIcon button': {
                            zIndex: 999,
                            mr: 1,
                        },
                    }}
                />
            </Box>
        </>
    );
}
