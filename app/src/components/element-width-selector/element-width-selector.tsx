import {Box, ListSubheader, MenuItem, TextField, Typography} from '@mui/material';
import {alpha} from '@mui/material/styles';
import React, {useMemo} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {
    ElementWidthChoice,
    getElementWidthChoices,
    getElementWidthRestrictionHint,
    normalizeElementWeight,
} from '../../utils/element-widths';

interface ElementWidthSelectorProps {
    label: string;
    elementType: ElementType;
    value: number | null | undefined;
    onChange: (value: number) => void;
    hint?: string;
    disabled?: boolean;
}

function ElementWidthBar(props: {
    choice: ElementWidthChoice;
    muted?: boolean;
}) {
    const {
        choice,
        muted = false,
    } = props;

    const hasRemainingWidth = choice.value < 12;

    return (
        <Box
            sx={{
                display: 'grid',
                gap: 1,
                gridTemplateColumns: hasRemainingWidth ? `${choice.value}fr ${12 - choice.value}fr` : '1fr',
                minWidth: 0,
                width: '100%',
            }}
        >
            <Box
                sx={(theme) => ({
                    backgroundColor: muted
                        ? alpha(theme.palette.text.secondary, 0.18)
                        : alpha(theme.palette.primary.dark, 0.50),
                    borderRadius: 999,
                    height: 8,
                    minWidth: 10,
                })}
            />

            {
                hasRemainingWidth &&
                <Box
                    sx={(theme) => ({
                        backgroundColor: muted
                            ? alpha(theme.palette.text.secondary, 0.08)
                            : alpha(theme.palette.text.primary, 0.14),
                        borderRadius: 999,
                        height: 8,
                        minWidth: 10,
                    })}
                />
            }
        </Box>
    );
}

function ElementWidthOptionContent(props: {
    choice: ElementWidthChoice;
    variant: 'field' | 'menu';
}) {
    const {
        choice,
        variant,
    } = props;

    const compact = variant === 'field';

    return (
        <Box
            sx={{
                display: 'grid',
                alignItems: 'center',
                columnGap: compact ? 1.25 : 1.5,
                gridTemplateColumns: compact ? '68px minmax(120px, 1fr) 60px' : '68px minmax(180px, 1fr) 60px',
                minWidth: 0,
                width: '100%',
            }}
        >
            <Typography
                className="element-width-option-label"
                fontSize={16}
                fontWeight={400}
                noWrap
                sx={{
                    color: choice.disabled ? 'text.disabled' : 'text.primary',
                    fontVariantNumeric: 'tabular-nums',
                    minWidth: 0,
                    width: '68px',
                }}
            >
                {choice.label}
            </Typography>

            <Box
                sx={{
                    alignItems: 'center',
                    display: 'flex',
                    minWidth: 0,
                }}
            >
                <ElementWidthBar
                    choice={choice}
                    muted={choice.disabled}
                />
            </Box>

            <Typography
                fontSize={12}
                noWrap
                sx={{
                    color: choice.disabled ? 'text.disabled' : 'text.secondary',
                    justifySelf: 'end',
                    fontVariantNumeric: 'tabular-nums',
                    textAlign: 'right',
                    width: '60px',
                }}
            >
                {choice.fractionLabel}
            </Typography>
        </Box>
    );
}

export function ElementWidthSelector(props: ElementWidthSelectorProps) {
    const {
        label,
        elementType,
        value,
        onChange,
        hint,
        disabled = false,
    } = props;

    const normalizedValue = useMemo(() => normalizeElementWeight(elementType, value), [elementType, value]);
    const choices = useMemo(() => getElementWidthChoices(elementType), [elementType]);
    const selectedChoice = useMemo(() => {
        return choices.find(choice => choice.value === normalizedValue) ?? choices[choices.length - 1];
    }, [choices, normalizedValue]);
    const restrictionHint = useMemo(() => getElementWidthRestrictionHint(elementType), [elementType]);
    const helperText = useMemo(() => {
        return [
            hint,
            restrictionHint,
        ]
            .filter((part): part is string => part != null && part.length > 0)
            .join(' ');
    }, [hint, restrictionHint]);
    const hasDisabledChoices = useMemo(() => choices.some(choice => choice.disabled), [choices]);

    return (
        <TextField
            fullWidth
            select
            label={label}
            value={normalizedValue.toString()}
            onChange={(event) => {
                const nextChoice = choices.find(choice => choice.value.toString() === event.target.value);

                if (nextChoice != null && !nextChoice.disabled) {
                    onChange(nextChoice.value);
                }
            }}
            disabled={disabled}
            helperText={helperText}
            InputLabelProps={{
                title: label,
            }}
            SelectProps={{
                renderValue: () => (
                    <ElementWidthOptionContent
                        choice={selectedChoice}
                        variant="field"
                    />
                ),
                MenuProps: {
                    PaperProps: {
                        sx: {
                            minWidth: 360,
                        },
                    },
                },
            }}
            sx={{
                '& .MuiOutlinedInput-root': {
                    minHeight: 56,
                },
                '& .MuiSelect-select': {
                    alignItems: 'center',
                    display: 'flex',
                    minHeight: '24px !important',
                    py: '16.5px',
                },
                '& .MuiSelect-select > .MuiBox-root': {
                    width: '100%',
                },
            }}
        >
            {
                hasDisabledChoices && restrictionHint != null &&
                <ListSubheader
                    disableSticky
                    sx={{
                        color: 'text.secondary',
                        fontSize: 12,
                        lineHeight: 1.4,
                        py: 1.25,
                        whiteSpace: 'normal',
                    }}
                >
                    {restrictionHint}
                </ListSubheader>
            }
            {
                choices.map((choice) => (
                    <MenuItem
                        key={choice.value}
                        value={choice.value.toString()}
                        disabled={choice.disabled}
                        sx={{
                            py: 1.25,
                            '&.Mui-disabled': {
                                opacity: 1,
                            },
                            '&.Mui-selected': {
                                backgroundColor: 'action.selected',
                            },
                            '&.Mui-selected:hover': {
                                backgroundColor: 'action.selected',
                            },
                        }}
                    >
                        <ElementWidthOptionContent
                            choice={choice}
                            variant="menu"
                        />
                    </MenuItem>
                ))
            }
        </TextField>
    );
}
