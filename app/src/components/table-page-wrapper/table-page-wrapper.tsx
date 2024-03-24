import React, {type PropsWithChildren} from 'react';
import {type TablePageWrapperProps} from './table-page-wrapper-props';
import {PageWrapper} from '../page-wrapper/page-wrapper';
import {type GridValidRowModel} from '@mui/x-data-grid';
import {TableWrapper} from '../table-wrapper/table-wrapper';

export function TablePageWrapper<T extends GridValidRowModel>(props: PropsWithChildren<TablePageWrapperProps<T>>): JSX.Element {
    const {
        columns,
        rows,
        onRowClick,
        getRowId,

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
        <PageWrapper {...pageWrapperProps}>
            <TableWrapper
                columns={columns}
                rows={rows}
                onRowClick={onRowClick}
                title={pageWrapperProps.title}
                search={search}
                searchPlaceholder={searchPlaceholder}
                onSearchChange={onSearchChange}
                actions={actions}
                hint={hint}
                smallTitle={smallTitle}
                getRowId={getRowId}
            >
                {children}
            </TableWrapper>
        </PageWrapper>
    );
}
