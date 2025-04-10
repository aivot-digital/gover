import React, {useMemo} from 'react';
import {FormControl, FormControlLabel, FormHelperText, FormLabel, Radio, RadioGroup} from '@mui/material';
import {type RadioFieldElement} from '../../models/elements/form/input/radio-field-element';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {type SelectFieldElementOption} from '../../models/elements/form/input/select-field-element';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';

export function RadioFieldComponentView(props: BaseViewProps<RadioFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        disabled,
        options: baseOptions,
    } = element;

    const options = useMemo(() => {
        if (baseOptions == null) {
            return [];
        }

        return baseOptions
            .map((option: string | SelectFieldElementOption) => {
                if (typeof option === 'string') {
                    return {
                        value: option,
                        label: option,
                    };
                } else {
                    return option;
                }
            });
    }, [baseOptions]);

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <FormControl
            error={error != null}
            disabled={isDisabled}
        >
            <FormLabel
                id={'label-' + element.id}
            >
                {element.label} {element.required && ' *'}
            </FormLabel>
            <RadioGroup
                aria-labelledby={'label-' + element.id}
                name={'radio-group-' + element.id}
                value={value ?? ''}
                onChange={(event) => {
                    if (!isBusy) {
                        if (isStringNullOrEmpty(event.target.value)) {
                            setValue(undefined);
                        } else {
                            setValue(event.target.value ?? '');
                        }
                    }
                }}
                row={element.displayInline ?? false}
            >
                {
                    !element.required &&
                    <FormControlLabel
                        value={''}
                        control={<Radio
                            sx={{color: isBusy ? "rgba(0, 0, 0, 0.26)!important" : undefined}}
                        />}
                        label="Keine Auswahl"
                        disabled={isDisabled}
                        sx={{
                            ...(isBusy ? {
                                color: "rgba(0, 0, 0, 0.38)!important",
                                cursor: "not-allowed",
                            } : {}),
                            fontStyle: 'italic',
                            mr: element.displayInline ? 3 : undefined
                        }}
                    />
                }
                {
                    options.map((option) => (
                        <FormControlLabel
                            key={option.value}
                            value={option.value}
                            control={<Radio
                                sx={{color: isBusy ? "rgba(0, 0, 0, 0.26)!important" : undefined}}
                            />}
                            label={option.label}
                            disabled={isDisabled}
                            sx={{
                                ...(element.displayInline ? { mr: 3 } : {}),
                                ...(isBusy ? {
                                    color: "rgba(0, 0, 0, 0.38)!important",
                                    cursor: "not-allowed",
                                } : {}),
                                '& .MuiFormControlLabel-label': {
                                    wordBreak: 'break-word',
                                    whiteSpace: 'normal',
                                },
                            }}
                        />
                    ))
                }
            </RadioGroup>
            {
                (element.hint != null || error != null) &&
                <FormHelperText sx={{ml: 0}}>
                    {
                        element.hint != null &&
                        error == null &&
                        element.hint
                    }
                    {
                        error != null &&
                        <span>{error}</span>
                    }
                </FormHelperText>
            }

        </FormControl>
    );
}
