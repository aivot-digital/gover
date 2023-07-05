import {TablePageWrapperProps} from "./table-page-wrapper-props";
import React, {PropsWithChildren} from "react";
import {PageWrapper} from "../page-wrapper/page-wrapper";
import {DataGrid, GridValidRowModel} from "@mui/x-data-grid";
import {ListHeader} from "../list-header/list-header";
import {Box} from "@mui/material";

export function TablePageWrapper<T extends GridValidRowModel>(props: PropsWithChildren<TablePageWrapperProps<T>>) {
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
        <PageWrapper {...pageWrapperProps}>
            <ListHeader
                title={pageWrapperProps.title}
                search={search}
                searchPlaceholder={searchPlaceholder}
                onSearchChange={onSearchChange}
                actions={actions}
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
                    onRowClick={event => onRowClick(event.row)}
                    disableSelectionOnClick={true}
                    disableColumnFilter={true}
                    disableColumnMenu={true}
                />
            </Box>
        </PageWrapper>
    );
}