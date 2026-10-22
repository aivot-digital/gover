import type {ReactNode} from 'react';
import {Box, ButtonBase, IconButton, type SxProps, type Theme, Tooltip, Typography} from '@mui/material';
import {getFileTypeIconForFile} from '../../utils/file-type-icon';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface FileListAction {
    label: string;
    icon: ReactNode;
    onClick: () => void;
    disabled?: boolean;
}

export interface FileListItem {
    key: string;
    name: string;
    contentType?: string | null;
    secondaryText?: string;
    onView?: () => void;
    actions?: FileListAction[];
}

export function FileList({items, sx}: {items: FileListItem[]; sx?: SxProps<Theme>}) {
    if (items.length === 0) {
        return null;
    }

    return (
        <Box role="list" sx={[
            {overflow: 'hidden', border: 1, borderColor: 'divider', borderRadius: 1},
            ...(Array.isArray(sx) ? sx : [sx]),
        ]}>
            {items.map((item, index) => {
                const name = (
                    <Typography component="span" variant="body2" title={item.name} noWrap sx={{
                        display: 'block',
                        maxWidth: '100%',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        fontWeight: 500,
                        lineHeight: 1.35,
                    }}>
                        {item.name}
                    </Typography>
                );

                return (
                    <Box key={item.key} role="listitem" sx={{
                        display: 'grid',
                        gridTemplateColumns: item.actions?.length ? 'auto minmax(0, 1fr) auto' : 'auto minmax(0, 1fr)',
                        alignItems: 'center',
                        gap: 1,
                        minHeight: FormFieldTokens.groupedControlRowMinHeight,
                        px: 1.5,
                        py: 0.5,
                        borderTop: index === 0 ? 0 : '1px solid',
                        borderColor: 'divider',
                    }}>
                        {getFileTypeIconForFile(item.name, item.contentType, {
                            fontSize: 'small',
                            sx: {flexShrink: 0, color: 'text.secondary'},
                        })}
                        <Box sx={{minWidth: 0}}>
                            {item.onView ? (
                                <Tooltip title="In neuem Tab ansehen" arrow>
                                    <ButtonBase
                                        aria-label={`${item.name} ansehen`}
                                        onClick={item.onView}
                                        sx={{
                                            maxWidth: '100%',
                                            minWidth: 0,
                                            textAlign: 'left',
                                            borderRadius: 0.5,
                                            '&.Mui-focusVisible': {
                                                outline: '2px solid',
                                                outlineColor: 'primary.main',
                                            },
                                        }}
                                    >
                                        {name}
                                    </ButtonBase>
                                </Tooltip>
                            ) : name}
                            {item.secondaryText && (
                                <Typography component="div" variant="caption" sx={{
                                    color: 'text.secondary', overflowWrap: 'anywhere', lineHeight: 1.2,
                                }}>
                                    {item.secondaryText}
                                </Typography>
                            )}
                        </Box>
                        {(item.actions?.length ?? 0) > 0 && (
                            <Box sx={{display: 'flex', alignItems: 'center'}}>
                                {item.actions?.map(action => (
                                    <Tooltip key={action.label} title={action.label} arrow>
                                        <span>
                                            <IconButton
                                                size="small"
                                                aria-label={action.label}
                                                onClick={action.onClick}
                                                disabled={action.disabled}
                                            >
                                                {action.icon}
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                ))}
                            </Box>
                        )}
                    </Box>
                );
            })}
        </Box>
    );
}
