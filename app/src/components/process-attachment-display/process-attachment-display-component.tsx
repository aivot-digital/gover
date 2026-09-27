import React, {type ReactNode} from 'react';
import {
    Box,
    ButtonBase,
    CircularProgress,
    FormControl,
    FormHelperText,
    FormLabel,
    IconButton,
    Paper,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import type {SxProps, Theme} from '@mui/material';
import AttachFile from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import Description from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';

export interface ProcessAttachmentDisplayItem {
    key: string;
    fileName: string;
    originalFileName: string;
    group?: string | undefined | null;
    onView?: () => void;
    onDownload?: () => void;
}

interface ProcessAttachmentDisplayComponentProps {
    items?: ProcessAttachmentDisplayItem[];
    labelText?: string | null;
    statusText?: string;
    hintText?: string | null;
    previewText?: string;
    loading?: boolean;
}

export function ProcessAttachmentDisplayComponent(props: ProcessAttachmentDisplayComponentProps): React.JSX.Element {
    const items = props.items ?? [];

    return (
        <FormControl fullWidth>
            <FormLabel sx={{mb: 1}}>
                {props.labelText == null || props.labelText.trim().length === 0 ? 'Anhang zum Vorgang' : props.labelText}
            </FormLabel>

            <Box sx={{containerType: 'inline-size'}}>
                {
                    items.length > 0 &&
                    <Box
                        sx={{
                            'display': 'grid',
                            'gridTemplateColumns': 'minmax(0, 1fr)',
                            'gap': 1,
                            '@container (min-width: 720px)': {
                                gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                            },
                        }}
                    >
                        <AttachmentItems items={items}/>
                    </Box>
                }
                {
                    items.length === 0 &&
                    <AttachmentStatus loading={props.loading}>
                        {props.statusText}
                    </AttachmentStatus>
                }
            </Box>

            {
                props.hintText != null && props.hintText.trim().length > 0 &&
                <FormHelperText
                    sx={{
                        ml: 0,
                        mt: 0.75,
                    }}
                >
                    {props.hintText}
                </FormHelperText>
            }

            {
                props.previewText != null &&
                <Typography
                    variant="caption"
                    sx={{
                        color: "text.secondary",
                        mt: 0.75
                    }}>
                    {props.previewText}
                </Typography>
            }
        </FormControl>
    );
}

interface AttachmentItemsProps {
    items: ProcessAttachmentDisplayItem[];
}

function AttachmentItems(props: AttachmentItemsProps): React.JSX.Element {
    const itemsByGroup = new Map<string, ProcessAttachmentDisplayItem[]>();
    for (const item of props.items) {
        const group = normalizeAttachmentGroup(item.group);
        if (group == null) {
            continue;
        }

        const groupItems = itemsByGroup.get(group) ?? [];
        groupItems.push(item);
        itemsByGroup.set(group, groupItems);
    }

    if (itemsByGroup.size === 0) {
        return (
            <>
                {
                    props.items.map((item) => (
                        <AttachmentItem
                            key={item.key}
                            item={item}
                        />
                    ))
                }
            </>
        );
    }

    const renderedGroups = new Set<string>();

    return (
        <>
            {
                props.items.map((item) => {
                    const group = normalizeAttachmentGroup(item.group);

                    if (group == null) {
                        return (
                            <AttachmentItem
                                key={item.key}
                                item={item}
                            />
                        );
                    }

                    if (renderedGroups.has(group)) {
                        return null;
                    }

                    renderedGroups.add(group);

                    return (
                        <Paper
                            key={`group:${group}`}
                            aria-label="Anlagengruppe"
                            role="group"
                            variant="outlined"
                            sx={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 1,
                                minWidth: 0,
                                p: 1,
                            }}
                        >
                            <Typography variant="caption" color="textSecondary">
                                Gruppenkennzeichnung: {group}
                            </Typography>

                            {
                                itemsByGroup.get(group)?.map((groupedItem) => (
                                    <AttachmentItem
                                        key={groupedItem.key}
                                        item={groupedItem}
                                    />
                                ))
                            }
                        </Paper>
                    );
                })
            }
        </>
    );
}

function normalizeAttachmentGroup(group: string | null | undefined): string | null {
    const normalizedGroup = group?.trim();
    return normalizedGroup == null || normalizedGroup.length === 0 ? null : normalizedGroup;
}

interface AttachmentItemProps {
    item: ProcessAttachmentDisplayItem;
}

function AttachmentItem(props: AttachmentItemProps): React.JSX.Element {
    const fileNameActionSx: SxProps<Theme> = {
        display: 'inline-flex',
        width: 'fit-content',
        minWidth: 0,
        maxWidth: '100%',
        justifyContent: 'flex-start',
        textAlign: 'left',
        borderRadius: 0.5,
        py: 1,
    };

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                minWidth: 0,
                minHeight: 48,
                px: 1.5,
                gap: 1,
                border: 1,
                borderColor: 'divider',
                borderRadius: 1,
                backgroundColor: 'background.paper',
            }}
        >
            <Description
                color="primary"
                sx={{flexShrink: 0}}
            />
            <Box
                sx={{
                    display: 'flex',
                    flexGrow: 1,
                    minWidth: 0,
                }}
            >
                {
                    props.item.onView != null ?
                        <Tooltip
                            title="In neuem Tab ansehen"
                            arrow
                        >
                            <ButtonBase
                                aria-label={`${props.item.fileName} ansehen`}
                                onClick={props.item.onView}
                                sx={fileNameActionSx}
                            >
                                <FileName
                                    fileName={props.item.fileName}
                                    original={props.item.originalFileName}
                                />
                            </ButtonBase>
                        </Tooltip> :
                        <FileName
                            fileName={props.item.fileName}
                            original={props.item.originalFileName}
                        />
                }
            </Box>

            {
                props.item.onView != null ?
                    <Tooltip
                        title="In neuem Tab ansehen"
                        arrow
                    >
                        <IconButton
                            aria-label={`${props.item.fileName} in neuem Tab ansehen`}
                            onClick={props.item.onView}
                            size="small"
                            color="primary"
                        >
                            <OpenInNew fontSize="small"/>
                        </IconButton>
                    </Tooltip> :
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            p: 1,
                            color: 'text.disabled',
                        }}
                    >
                        <OpenInNew fontSize="small"/>
                    </Box>
            }

            {
                props.item.onDownload != null ?
                    <Tooltip
                        title="Herunterladen"
                        arrow
                    >
                        <IconButton
                            aria-label={`${props.item.fileName} herunterladen`}
                            onClick={props.item.onDownload}
                            size="small"
                            sx={{color: 'text.secondary'}}
                        >
                            <Download fontSize="small"/>
                        </IconButton>
                    </Tooltip> :
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            p: 1,
                            color: 'text.disabled',
                        }}
                    >
                        <Download fontSize="small"/>
                    </Box>
            }
        </Box>
    );
}

function FileName(props: { fileName: string, original: string }): React.JSX.Element {
    return (
        <Stack direction="column" spacing={0.25}>
            <Typography
                component="span"
                variant="body2"
                sx={{
                    display: 'block',
                    maxWidth: '100%',
                    minWidth: 0,
                    fontWeight: 500,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                }}
            >
                {props.fileName}
            </Typography>

            <Typography
                variant="caption"
                color="textSecondary"
            >
                Hochgeladen als: {props.original}
            </Typography>
        </Stack>
    );
}

interface AttachmentStatusProps {
    children?: ReactNode;
    loading?: boolean;
}

function AttachmentStatus(props: AttachmentStatusProps): React.JSX.Element {
    return (
        <Stack
            direction="row"
            spacing={1.25}
            sx={{
                alignItems: "center",
                minHeight: 48,
                px: 1.5,
                py: 1,
                border: 1,
                borderStyle: 'dashed',
                borderColor: 'divider',
                borderRadius: 1,
                backgroundColor: 'action.hover'
            }}>
            {
                props.loading === true ?
                    <CircularProgress size={20}/> :
                    <AttachFile color="disabled"/>
            }
            <Typography
                variant="body2"
                sx={{
                    color: "text.secondary"
                }}
            >
                {props.children}
            </Typography>
        </Stack>
    );
}
