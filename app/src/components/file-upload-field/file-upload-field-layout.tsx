import React, {
    type AriaAttributes,
    type ChangeEventHandler,
    type DragEventHandler,
    type FocusEventHandler,
    type ReactNode,
    type RefObject,
} from 'react';
import {alpha, Box, Button, type SxProps, type Theme, Typography} from '@mui/material';
import UploadFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/UploadFile';
import {pluralize} from '../../utils/humanization-utils';
import {FileList} from '../file-list/file-list';
import {
    FormField,
    type FormFieldControlContext,
    type FormFieldLayoutProps,
    getNativeInputAriaProps,
} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

export interface FileUploadInputAreaProps {
    id: string;
    inputRef: RefObject<HTMLInputElement | null>;
    multiple?: boolean;
    extensions?: string[] | null;
    required?: boolean;
    disabled: boolean;
    error: boolean;
    focused: boolean;
    draggedOver: boolean;
    placeholder?: string;
    inputAriaProps?: AriaAttributes;
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
            data-file-upload-input-area
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
                    xs: 'minmax(0, 1fr)',
                    sm: 'minmax(0, 1fr) auto',
                },
                alignItems: 'center',
                columnGap: 1,
                rowGap: 1,
                minHeight: 64,
                px: 1.5,
                py: 1,
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
                required={props.required}
                accept={props.extensions != null ? props.extensions.map(ext => `.${ext}`).join(',') : undefined}
                {...props.inputAriaProps}
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
                sx={{
                    minWidth: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    gap: 0.25,
                }}
            >
                <Typography
                    component="div"
                    sx={{
                        color: props.disabled ? 'text.disabled' : 'text.primary',
                        fontSize: '1rem',
                        lineHeight: 1.25,
                    }}
                >
                    {prompt}
                </Typography>

                {
                    props.extensions != null && props.extensions.length > 0 &&
                    <Typography
                        component="div"
                        variant="caption"
                        sx={{
                            color: props.disabled ? 'text.disabled' : 'text.secondary',
                            overflowWrap: 'anywhere',
                            fontSize: '0.75rem',
                            lineHeight: 1.2,
                        }}
                    >
                        Erlaubte Formate: {props.extensions.map(ext => `.${ext}`).join(', ')}
                    </Typography>
                }
            </Box>

            <Button
                variant="text"
                size="small"
                startIcon={<UploadFileOutlinedIcon />}
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
    contentType?: string | null;
    detail?: string;
    actionLabel: string;
    actionIcon: ReactNode;
    actionDisabled?: boolean;
    onAction: () => void;
}

export function FileUploadFileList({items}: {items: FileUploadListItem[]}) {
    return (
        <FileList
            sx={{mb: 1}}
            items={items.map(item => ({
                key: item.key,
                name: item.name,
                contentType: item.contentType,
                secondaryText: [item.size, item.detail].filter(Boolean).join(' · '),
                actions: [{
                    label: item.actionLabel,
                    icon: item.actionIcon,
                    onClick: item.onAction,
                    disabled: item.actionDisabled,
                }],
            }))}
        />
    );
}

export interface FileUploadFieldLayoutProps extends FormFieldLayoutProps {
    label: string;
    required?: boolean;
    disabled?: boolean;
    busy?: boolean;
    error?: string;
    hint?: string | null;
    fileCount: number;
    minFiles?: number | null;
    maxFiles?: number | null;
    items: FileUploadListItem[];
    showInput: boolean;
    inputAreaProps: Omit<FileUploadInputAreaProps, 'id' | 'inputAriaProps'>;
    controlSx?: SxProps<Theme>;
}

export function FileUploadFieldLayout(props: FileUploadFieldLayoutProps) {
    const hasError = props.error != null && props.error.length > 0;
    const countHint = getFileCountHint(props.fileCount, props.minFiles, props.maxFiles);
    const hasHint = props.hint != null && props.hint.length > 0;
    const hasHelperContent = hasError || hasHint || countHint != null;
    const helperContent = hasHelperContent ? (
        <FileUploadHelper
            error={hasError ? props.error : undefined}
            hint={!hasError ? props.hint : undefined}
            fileCount={props.fileCount}
            minFiles={props.minFiles}
            maxFiles={props.maxFiles}
        />
    ) : undefined;

    return (
        <FormField
            id={props.id}
            label={props.label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            externalAction={props.externalAction}
            labelAction={props.labelAction}
            hint={!hasError ? helperContent : undefined}
            error={hasError ? helperContent : undefined}
            required={props.required}
            disabled={props.disabled}
            busy={props.busy}
            margin={props.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(fieldContext: FormFieldControlContext) => (
                <Box
                    id={props.showInput ? `${fieldContext.controlId}-group` : fieldContext.controlId}
                    role={!props.showInput ? 'group' : undefined}
                    aria-labelledby={!props.showInput ? fieldContext.labelId : undefined}
                    aria-describedby={!props.showInput ? fieldContext.ariaProps['aria-describedby'] : undefined}
                    aria-disabled={!props.showInput ? fieldContext.ariaProps['aria-disabled'] : undefined}
                    aria-busy={!props.showInput ? fieldContext.ariaProps['aria-busy'] : undefined}
                    aria-invalid={!props.showInput ? fieldContext.ariaProps['aria-invalid'] : undefined}
                    sx={props.controlSx}
                >
                    <FileUploadFileList items={props.items}/>

                    {props.showInput && (
                        <FileUploadInputArea
                            {...props.inputAreaProps}
                            id={fieldContext.controlId}
                            required={fieldContext.required}
                            inputAriaProps={getNativeInputAriaProps(fieldContext)}
                        />
                    )}
                </Box>
            )}
        </FormField>
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
            component="span"
            sx={{
                display: 'flex',
                alignItems: 'baseline',
                flexWrap: 'wrap',
                columnGap: 2,
                rowGap: 0.25,
                width: '100%',
            }}
        >
            {
                (props.error != null || props.hint != null) &&
                <Box component="span" sx={{minWidth: 0}}>
                    {props.error ?? props.hint}
                </Box>
            }

            {(props.error != null || props.hint != null) && countHint != null && ' '}

            {
                countHint != null &&
                <Box
                    component="span"
                    sx={{ml: 'auto', color: props.error != null ? 'text.secondary' : 'inherit'}}
                >
                    {countHint}
                </Box>
            }
        </Box>
    );
}

export function getFileCountHint(
    fileCount: number,
    minFiles?: number | null,
    maxFiles?: number | null,
): string | null {
    if (maxFiles != null && maxFiles > 0) {
        const qualifier = minFiles === maxFiles ? '' : 'max. ';
        return `${fileCount} von ${qualifier}${maxFiles} ${pluralize(maxFiles, 'Datei', 'Dateien')}`;
    }

    if (minFiles != null && minFiles > 0) {
        return `Mindestens ${minFiles} ${pluralize(minFiles, 'Datei', 'Dateien')}`;
    }

    return null;
}
