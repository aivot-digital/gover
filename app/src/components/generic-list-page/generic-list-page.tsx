import {GridValidRowModel} from '@mui/x-data-grid';
import {Container, Paper} from '@mui/material';
import React, {useState} from 'react';
import {GenericPageHeader} from '../generic-page-header/generic-page-header';
import {GenericListProps} from "../generic-list/generic-list-props";
import {GenericPageHeaderProps} from "../generic-page-header/generic-page-header-props";
import {GenericList} from "../generic-list/generic-list";
import {GenericListRowModel} from "../generic-list/generic-list-row-models";

interface GenericListPageProps<ItemType extends GridValidRowModel> extends GenericListProps<ItemType> {
    header: GenericPageHeaderProps;
}

export function GenericListPage<ItemType extends GenericListRowModel>(props: GenericListPageProps<ItemType>) {
    const { header, ...listProps } = props;
    const [isFullWidth, setIsFullWidth] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    return (
        <Container maxWidth={isFullWidth ? false : 'lg'}>
            <GenericPageHeader {...header} isBusy={isBusy} />

            <Paper
                sx={{
                    marginTop: 3.5,
                }}
            >
                <GenericList
                    {...listProps}
                    onFullWidthChange={setIsFullWidth}
                    onBusyChange={setIsBusy}
                />
            </Paper>
        </Container>
    );
}