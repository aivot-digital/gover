import React, {type ChangeEventHandler, type DragEventHandler, type FocusEventHandler, type ReactNode, type RefObject} from 'react';
import {alpha, Box, Button, FormHelperText, IconButton, Tooltip, Typography} from '@mui/material';
import UploadFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/UploadFile';
import DraftOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Draft';
import {pluralize} from '../../utils/humanization-utils';

export interface FileUploadInputAreaProps {
    id: string;
    inputRef: RefObject<HTMLInputElement | null>;
    multiple?: boolean;
    extensions?: string[] | null;
    disabled: boolean;
    error: boolean;
    focused: boolean;
    draggedOver: boolean;
    placeholder?: string;
    onChange: ChangeEventHandler<HTMLInputElement>;
    onFocus: FocusEventHandler<HTMLInputElement>;
    onBlur: FocusEventHandler<HTMLInputElement>;
    onDragOver: DragEventHandler<HTMLDivElement>;
    onDragLeave: DragEventHandler<HTMLDivElement>;
    onDrop: DragEventHandler<HTMLDivElement>;
}

export function FileUploadInputArea(props: FileUploadInputAreaProps) {
    const prompt = props.placeholder?.trim() || (props.multiple
        ? 'Dateien auswählen oder hier ablegen'
        : 'Datei auswählen oder hier ablegen');

    return (
        <Box
            onDragOver={props.onDragOver}
            onDragLeave={(event) => {
                // Ignore transitions between descendants so the drag state does not flicker over the field content.
                if (event.relatedTarget instanceof Node && event.currentTarget.contains(event.relatedTarget)) {
                    return;
                }
                props.onDragLeave(event);
            }}
            onDrop={props.onDrop}
            sx={(theme) => ({
                display: 'grid',
                gridTemplateColumns: {
                    xs: 'auto minmax(0, 1fr)',
                    sm: 'auto minmax(0, 1fr) auto',
                },
                alignItems: 'center',
                columnGap: 1.5,
                rowGap: 1.25,
                minHeight: 88,
                px: 2,
                py: 1.5,
                border: '1px solid',
                borderColor: props.error
                    ? 'error.main'
                    : props.focused || props.draggedOver
                        ? 'primary.main'
                        : 'divider',
                borderRadius: 1,
                backgroundColor: props.draggedOver && !props.disabled
                    ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.14 : 0.07)
                    : 'transparent',
                transition: theme.transitions.create(['background-color', 'border-color', 'box-shadow'], {
                    duration: theme.transitions.duration.shorter,
                }),
                boxShadow: props.focused && !props.error
                    ? `0 0 0 1px ${theme.palette.primary.main}`
                    : undefined,
                cursor: props.disabled ? 'not-allowed' : 'default',
                '&:hover': props.disabled
                    ? undefined
                    : {
                        borderColor: props.error ? 'error.main' : 'text.secondary',
                    },
            })}
        >
            <input
                id={props.id}
                ref={props.inputRef}
                type="file"
                multiple={props.multiple}
                accept={props.extensions != null ? props.extensions.map(ext => `.${ext}`).join(',') : undefined}
                onChange={props.onChange}
                onFocus={props.onFocus}
                onBlur={props.onBlur}
                disabled={props.disabled}
                style={{
                    position: 'absolute',
                    width: 1,
                    height: 1,
                    padding: 0,
                    margin: -1,
                    overflow: 'hidden',
                    clip: 'rect(0 0 0 0)',
                    whiteSpace: 'nowrap',
                    border: 0,
                }}
            />

            <Box
                sx={(theme) => ({
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 40,
                    height: 40,
                    borderRadius: '50%',
                    color: props.disabled
                        ? 'text.disabled'
                        : props.draggedOver
                            ? 'primary.main'
                            : 'text.secondary',
                    backgroundColor: props.disabled
                        ? 'action.disabledBackground'
                        : props.draggedOver
                            ? alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.2 : 0.1)
                            : 'action.hover',
                })}
            >
                <UploadFileOutlinedIcon />
            </Box>

            <Box sx={{minWidth: 0}}>
                <Typography
                    variant="body2"
                    sx={{
                        color: props.disabled ? 'text.disabled' : 'text.primary',
                        fontWeight: 500,
                    }}
                >
                    {prompt}
                </Typography>

                {
                    props.extensions != null && props.extensions.length > 0 &&
                    <Typography
                        component="div"
                        variant="caption"
                        color={props.disabled ? 'text.disabled' : 'text.secondary'}
                        sx={{mt: 0.25, overflowWrap: 'anywhere'}}
                    >
                        Erlaubte Formate: {props.extensions.map(ext => `.${ext}`).join(', ')}
                    </Typography>
                }
            </Box>

            <Button
                variant="outlined"
                onClick={() => props.inputRef.current?.click()}
                disabled={props.disabled}
                sx={{
                    gridColumn: {xs: '1 / -1', sm: 'auto'},
                    justifySelf: {xs: 'stretch', sm: 'end'},
                }}
            >
                {props.multiple ? 'Dateien auswählen' : 'Datei auswählen'}
            </Button>
        </Box>
    );
}

export interface FileUploadListItem {
    key: string;
    name: string;
    size: string;
    detail?: string;
    actionLabel: string;
    actionIcon: ReactNode;
    actionDisabled?: boolean;
    onAction: () => void;
}

export function FileUploadFileList({items}: {items: FileUploadListItem[]}) {
    if (items.length === 0) {
        return null;
    }

    return (
        <Box
            role="list"
            sx={{
                mb: 1.5,
                overflow: 'hidden',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
            }}
        >
            {
                items.map((item, index) => (
                    <Box
                        role="listitem"
                        key={item.key}
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: 'auto minmax(0, 1fr) auto',
                            alignItems: 'center',
                            gap: 1.25,
                            px: 1.5,
                            py: 1,
                            borderTop: index === 0 ? 0 : '1px solid',
                            borderColor: 'divider',
                        }}
                    >
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: 36,
                                height: 36,
                                flexShrink: 0,
                                borderRadius: '50%',
                                color: 'text.secondary',
                                backgroundColor: 'action.hover',
                            }}
                        >
                            <DraftOutlinedIcon fontSize="small" />
                        </Box>

                        <Box sx={{minWidth: 0}}>
                            <Typography
                                variant="body2"
                                title={item.name}
                                noWrap
                                sx={{
                                    display: 'block',
                                    maxWidth: '100%',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    fontWeight: 500,
                                }}
                            >
                                {item.name}
                            </Typography>
                            <Typography
                                component="div"
                                variant="caption"
                                color="text.secondary"
                                sx={{overflowWrap: 'anywhere'}}
                            >
                                {item.size}{item.detail != null ? ` · ${item.detail}` : ''}
                            </Typography>
                        </Box>

                        <Tooltip title={item.actionLabel} arrow>
                            <span>
                                <IconButton
                                    size="small"
                                    aria-label={item.actionLabel}
                                    onClick={item.onAction}
                                    disabled={item.actionDisabled}
                                >
                                    {item.actionIcon}
                                </IconButton>
                            </span>
                        </Tooltip>
                    </Box>
                ))
            }
        </Box>
    );
}

interface FileUploadHelperProps {
    error?: string;
    hint?: string | null;
    fileCount: number;
    minFiles?: number | null;
    maxFiles?: number | null;
}

export function FileUploadHelper(props: FileUploadHelperProps) {
    const countHint = getFileCountHint(props.fileCount, props.minFiles, props.maxFiles);

    if (props.error == null && props.hint == null && countHint == null) {
        return null;
    }

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'baseline',
                flexWrap: 'wrap',
                columnGap: 2,
                rowGap: 0.25,
                mt: 0.5,
                mx: 1.75,
            }}
        >
            {
                (props.error != null || props.hint != null) &&
                <FormHelperText
                    error={props.error != null}
                    sx={{m: 0}}
                >
                    {props.error ?? props.hint}
                </FormHelperText>
            }

            {
                countHint != null &&
                <FormHelperText sx={{m: 0, ml: 'auto'}}>
                    {countHint}
                </FormHelperText>
            }
        </Box>
    );
}

function getFileCountHint(fileCount: number, minFiles?: number | null, maxFiles?: number | null): string | null {
    if (maxFiles != null && maxFiles > 0) {
        const qualifier = minFiles === maxFiles ? '' : 'max. ';
        return `${fileCount} von ${qualifier}${maxFiles} ${pluralize(maxFiles, 'Datei', 'Dateien')}`;
    }

    if (minFiles != null && minFiles > 0) {
        return `Mindestens ${minFiles} ${pluralize(minFiles, 'Datei', 'Dateien')}`;
    }

    return null;
}
