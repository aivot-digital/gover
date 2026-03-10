import React, {type ReactNode} from 'react';
import {Box, Dialog, Tab, Tabs, type DialogProps} from '@mui/material';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

export interface SelectionDialogTabDefinition {
    label: ReactNode;
    value: number | string;
    disabled?: boolean;
    hidden?: boolean;
}

interface SelectionDialogShellProps {
    open: boolean;
    onClose: () => void;
    onExited?: () => void;
    title: ReactNode;
    tabs: SelectionDialogTabDefinition[];
    activeTab: number | string;
    onTabChange: (value: number | string) => void;
    showDetailsPanel: boolean;
    children: ReactNode;
    detailsPanel?: ReactNode;
    compactMaxWidth?: DialogProps['maxWidth'];
    expandedMaxWidth?: DialogProps['maxWidth'];
    detailsColumnWidth?: string;
    height?: string | number;
    closeTooltip?: string;
}

export function SelectionDialogShell(props: SelectionDialogShellProps): ReactNode {
    const visibleTabs = props.tabs.filter((tab) => tab.hidden !== true);

    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            fullWidth
            maxWidth={props.showDetailsPanel ? (props.expandedMaxWidth ?? 'lg') : (props.compactMaxWidth ?? 'md')}
            TransitionProps={{
                onExited: props.onExited,
            }}
        >
            <DialogTitleWithClose
                onClose={props.onClose}
                closeTooltip={props.closeTooltip}
            >
                {props.title}
            </DialogTitleWithClose>

            <Tabs
                value={props.activeTab}
                onChange={(_, value) => {
                    props.onTabChange(value);
                }}
                sx={{
                    px: 2,
                    mt: -1.5,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                {
                    visibleTabs.map((tab) => (
                        <Tab
                            key={`${tab.value}`}
                            label={tab.label}
                            value={tab.value}
                            disabled={tab.disabled}
                        />
                    ))
                }
            </Tabs>

            <Box
                sx={{
                    display: 'grid',
                    gridTemplateColumns: props.showDetailsPanel ? `minmax(0, 1.2fr) ${props.detailsColumnWidth ?? 'minmax(320px, 0.8fr)'}` : 'minmax(0, 1fr)',
                    height: props.height ?? 'min(74vh, 820px)',
                }}
            >
                <Box
                    sx={{
                        minWidth: 0,
                        minHeight: 0,
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                    }}
                >
                    {props.children}
                </Box>

                {
                    props.showDetailsPanel && props.detailsPanel != null &&
                    <Box
                        sx={{
                            minWidth: 0,
                            minHeight: 0,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            display: 'flex',
                            flexDirection: 'column',
                            overflow: 'hidden',
                        }}
                    >
                        {props.detailsPanel}
                    </Box>
                }
            </Box>
        </Dialog>
    );
}
