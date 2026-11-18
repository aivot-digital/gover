import {Box, Radio, RadioGroup, type SxProps, type Theme, Typography} from '@mui/material';
import {alpha} from '@mui/material/styles';
import {useMemo} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {
    type ElementWidthChoice,
    getElementWidthChoices,
    getElementWidthRestrictionHint,
    normalizeElementWeight,
} from '../../utils/element-widths';
import {FormFieldGroup, type FormFieldGroupLayoutProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

interface ElementWidthSelectorProps extends FormFieldGroupLayoutProps {
    ariaLabel?: string;
    label: string;
    elementType: ElementType;
    value: number | null | undefined;
    onChange: (value: number) => void;
    hint?: string;
    disabled?: boolean;
    controlSx?: SxProps<Theme>;
}

function ElementWidthBar(props: {
    choice: ElementWidthChoice;
    muted?: boolean;
    selected?: boolean;
}) {
    const {
        choice,
        muted = false,
        selected = false,
    } = props;

    return (
        <Box
            aria-hidden="true"
            sx={{
                display: 'grid',
                gridTemplateColumns: choice.value < 12 ? `${choice.value}fr ${12 - choice.value}fr` : '1fr',
                gap: 0.25,
                width: '100%',
                height: 4,
            }}
        >
            <Box
                sx={(theme) => ({
                    height: '100%',
                    borderRadius: 999,
                    backgroundColor: muted
                        ? alpha(theme.palette.text.secondary, theme.palette.mode === 'dark' ? 0.10 : 0.18)
                        : selected
                            ? alpha(theme.palette.primary.main, 0.65)
                            : alpha(theme.palette.text.secondary, 0.28),
                })}
            />
            {choice.value < 12 && (
                <Box sx={(theme) => ({
                    height: '100%',
                    borderRadius: 999,
                    backgroundColor: alpha(theme.palette.text.primary, muted
                        ? theme.palette.mode === 'dark' ? 0.04 : 0.06
                        : 0.10),
                })}/>
            )}
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

    return (
        <FormFieldGroup
            id={props.id}
            label={label}
            hint={helperText}
            disabled={disabled}
            ariaDescribedBy={props.ariaDescribedBy}
            externalAction={props.externalAction}
            labelAction={props.labelAction}
            margin={props.margin}
            showOptionalIndicator={props.showOptionalIndicator ?? false}
            sx={props.sx}
        >
            {(field) => (
                <RadioGroup
                    name={`${field.groupId}-options`}
                    aria-label={props.ariaLabel}
                    aria-labelledby={props.ariaLabel ? undefined : field.labelId}
                    aria-describedby={field.describedBy}
                    value={selectedChoice.value}
                    onChange={(event) => {
                        const nextChoice = choices.find(choice => choice.value.toString() === event.target.value);
                        if (!disabled && nextChoice != null && !nextChoice.disabled) {
                            onChange(nextChoice.value);
                        }
                    }}
                    sx={[
                        {
                            display: 'grid',
                            gridTemplateColumns: 'repeat(6, minmax(0, 1fr))',
                            gap: 0.25,
                            p: 0.375,
                            height: FormFieldTokens.controlMinHeight,
                            boxSizing: 'border-box',
                            border: 1,
                            borderColor: 'divider',
                            borderRadius: 1,
                            overflow: 'hidden',
                        },
                        ...(Array.isArray(props.controlSx) ? props.controlSx : [props.controlSx]),
                    ]}
                >
                    {choices.map((choice) => {
                        const isDisabled = disabled || choice.disabled;
                        const isSelected = choice.value === selectedChoice.value;

                        return (
                            <Box
                                key={choice.value}
                                component="label"
                                sx={(theme) => {
                                    const selectedBackground = isDisabled
                                        ? theme.palette.action.selected
                                        : alpha(theme.palette.text.primary, theme.palette.mode === 'dark' ? 0.06 : 0.04);

                                    return {
                                        position: 'relative',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        justifyContent: 'center',
                                        gap: 0.25,
                                        minWidth: 0,
                                        minHeight: 0,
                                        boxSizing: 'border-box',
                                        pt: 0.5,
                                        pb: 0.75,
                                        px: 1,
                                        border: 1,
                                        borderColor: isSelected && !isDisabled
                                            ? alpha(theme.palette.primary.main, 0.30)
                                            : 'transparent',
                                        borderRadius: 0.75,
                                        bgcolor: isSelected ? selectedBackground : undefined,
                                        color: isDisabled
                                            ? theme.palette.mode === 'dark'
                                                ? alpha(theme.palette.text.primary, 0.28)
                                                : theme.palette.text.disabled
                                            : theme.palette.text.primary,
                                        cursor: isDisabled ? 'default' : 'pointer',
                                        ...(!isDisabled && {
                                            '&:hover': {
                                                bgcolor: isSelected
                                                    ? selectedBackground
                                                    : theme.palette.action.hover,
                                            },
                                            '&:has(.Mui-focusVisible)': {
                                                outline: '2px solid',
                                                outlineColor: 'primary.main',
                                                outlineOffset: -3,
                                            },
                                        }),
                                    };
                                }}
                            >
                                <Radio
                                    value={choice.value}
                                    disabled={isDisabled}
                                    size="small"
                                    // Keep native radio interaction while the segment shows selection and focus.
                                    sx={{position: 'absolute', inset: 0, p: 0, opacity: 0}}
                                    slotProps={{input: {'aria-describedby': field.describedBy}}}
                                />
                                <Typography component="span" sx={{
                                    fontSize: 14,
                                    lineHeight: 1.25,
                                    fontVariantNumeric: 'tabular-nums',
                                    fontWeight: 400,
                                    textAlign: 'center',
                                    whiteSpace: 'nowrap',
                                }}>
                                    {choice.label}
                                </Typography>
                                <ElementWidthBar choice={choice} muted={isDisabled} selected={isSelected}/>
                            </Box>
                        );
                    })}
                </RadioGroup>
            )}
        </FormFieldGroup>
    );
}
