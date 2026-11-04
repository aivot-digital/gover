import {type MouseEvent, type Ref} from 'react';
import {Box, ButtonBase, IconButton, Typography} from '@mui/material';
import Abc from '@aivot/mui-material-symbols-400-n25-outlined/Abc';
import CalendarClock from '@aivot/mui-material-symbols-400-n25-outlined/CalendarClock';
import CalendarToday from '@aivot/mui-material-symbols-400-n25-outlined/CalendarToday';
import Category from '@aivot/mui-material-symbols-400-n25-outlined/Category';
import Close from '@aivot/mui-material-symbols-400-n25-outlined/Close';
import DataArray from '@aivot/mui-material-symbols-400-n25-outlined/DataArray';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import Numbers from '@aivot/mui-material-symbols-400-n25-outlined/Numbers';
import Schedule from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import ToggleOn from '@aivot/mui-material-symbols-400-n25-outlined/ToggleOn';
import {NoCodeDataType, NoCodeDataTypeLabels} from '../../../data/no-code-data-type';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

export const NoCodeDataTypeIcons: Record<NoCodeDataType, typeof Abc> = {
    [NoCodeDataType.Runtime]: Category,
    [NoCodeDataType.Boolean]: ToggleOn,
    [NoCodeDataType.Number]: Numbers,
    [NoCodeDataType.String]: Abc,
    [NoCodeDataType.Date]: CalendarToday,
    [NoCodeDataType.DateTime]: CalendarClock,
    [NoCodeDataType.Time]: Schedule,
    [NoCodeDataType.List]: DataArray,
    [NoCodeDataType.Object]: DataObject,
};

const focusRing = {
    '&.Mui-focusVisible': {outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2},
};

export interface NoCodePillProps {
    dataType: NoCodeDataType;
    label: string;
    /** Accessible name of the dismiss button, naming what it deletes. */
    dismissLabel: string;
    onClick: (event: MouseEvent<HTMLButtonElement>) => void;
    onDismiss: () => void;
    error?: string | null;
    /** Lets a surrounding element, such as a tree item, reference the error as its description. */
    errorId?: string;
    /** A problem that does not stop evaluation, such as a type conflict. */
    warning?: string | null;
    warningId?: string;
    /** -1 inside a tree, whose roving tab index keeps a single tab stop. */
    tabIndex?: number;
    ref?: Ref<HTMLButtonElement>;
}

/**
 * The pill and its dismiss control are sibling buttons, because a button nested in a button is not operable for
 * assistive technology.
 */
export function NoCodePill(props: NoCodePillProps) {
    const {
        dataType,
        label,
        dismissLabel,
        onClick,
        onDismiss,
        error,
        warning,
        tabIndex,
        ref,
    } = props;

    const generatedId = useNormalizedReactId();
    const errorId = props.errorId ?? `${generatedId}-error`;
    const warningId = props.warningId ?? `${generatedId}-warning`;
    const Icon = NoCodeDataTypeIcons[dataType];
    const hasError = error != null && error.length > 0;
    const hasWarning = warning != null && warning.length > 0;
    const describedBy = [hasError && errorId, hasWarning && warningId].filter(Boolean).join(' ');

    return (
        <Box
            component="span"
            sx={{
                display: 'inline-flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: 0.5,
                maxWidth: '100%',
            }}
        >
            <Box
                component="span"
                sx={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    maxWidth: '100%',
                    height: 28,
                    borderRadius: 14,
                    border: 1,
                    borderColor: hasError ? 'error.main' : hasWarning ? 'warning.main' : 'divider',
                    bgcolor: 'action.hover',
                }}
            >
                <ButtonBase
                    ref={ref}
                    tabIndex={tabIndex}
                    aria-label={`${label}, ${NoCodeDataTypeLabels[dataType]}`}
                    aria-describedby={describedBy || undefined}
                    onClick={onClick}
                    sx={{
                        height: '100%',
                        minWidth: 0,
                        pl: 1,
                        pr: 0.5,
                        gap: 0.5,
                        borderRadius: 'inherit',
                        typography: 'body2',
                        ...focusRing,
                    }}
                >
                    <Icon
                        fontSize="small"
                        aria-hidden
                        sx={{color: hasError ? 'error.main' : 'text.secondary'}}
                    />
                    <Box
                        component="span"
                        sx={{
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {label}
                    </Box>
                </ButtonBase>

                <IconButton
                    size="small"
                    tabIndex={tabIndex}
                    aria-label={dismissLabel}
                    onClick={onDismiss}
                    sx={{
                        mr: 0.25,
                        p: 0.25,
                        ...focusRing,
                    }}
                >
                    <Close fontSize="inherit"/>
                </IconButton>
            </Box>

            {
                hasError &&
                <Typography
                    id={errorId}
                    variant="caption"
                    color="error"
                >
                    {error}
                </Typography>
            }

            {
                hasWarning &&
                <Typography
                    id={warningId}
                    variant="caption"
                    sx={{display: 'flex', alignItems: 'center', gap: 0.5}}
                >
                    <Warning fontSize="inherit" aria-hidden sx={{color: 'warning.main'}}/>
                    {warning}
                </Typography>
            }
        </Box>
    );
}
