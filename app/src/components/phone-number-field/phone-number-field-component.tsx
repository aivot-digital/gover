import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box} from '@mui/material';
import {MuiTelInput, type MuiTelInputCountry, type MuiTelInputInfo} from 'mui-tel-input';
import {PhoneNumberFieldComponentProps} from './phone-number-field-component-props';
import {isBlankPhoneNumber, normalizePhoneNumber} from '../../utils/phone-number-utils';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import {FormField, type FormFieldControlContext, getNativeInputAriaProps} from '../form-field';
import {FormFieldTokens} from '../../theming/form-field-tokens';

const preferredCountries: MuiTelInputCountry[] = ['DE', 'AT', 'CH'];

function cleanPhoneNumberValue(value: string | null | undefined): string | null {
    if (isBlankPhoneNumber(value)) {
        return null;
    }

    return value!.trim();
}

function getCanonicalPhoneNumber(value: string, info?: MuiTelInputInfo): string | null {
    if (info != null && isBlankPhoneNumber(info.nationalNumber)) {
        return null;
    }

    const normalizedInfoValue = cleanPhoneNumberValue(info?.numberValue);
    // Canonicalize plausible input; form-level validation decides whether the value is accepted.
    const normalizedValue = normalizePhoneNumber(normalizedInfoValue ?? value);

    return normalizedValue ?? cleanPhoneNumberValue(value);
}

function CountryCodeFlagElement(isoCode: MuiTelInputCountry, countryName: string | undefined) {
    return (
        <Box
            component="span"
            aria-label={countryName}
            title={countryName}
            sx={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 28,
                minWidth: 28,
                fontSize: '0.75rem',
                fontWeight: 600,
                letterSpacing: 0,
                color: 'text.secondary',
            }}
        >
            {isoCode}
        </Box>
    );
}

export function PhoneNumberFieldComponent(props: PhoneNumberFieldComponentProps) {
    const {
        label,
        placeholder,
        required,
        disabled,
        readonly,
        busy,
        value,
        error,
        hint,
        onChange,
        onBlur,
        controlSx,
        size = 'small',
        muiPassTroughProps,
    } = props;

    const [inputValue, setInputValue] = useState(value ?? '');
    const lastInputInfoRef = useRef<MuiTelInputInfo | undefined>(undefined);

    useEffect(() => {
        setInputValue(value ?? '');
    }, [value]);

    const errorMessages = useMemo(() => {
        if (error == null) {
            return [];
        }

        return (Array.isArray(error) ? error : [error])
            .filter((errorMessage) => errorMessage.length > 0);
    }, [error]);
    const helperText = errorMessages.length > 1 ? (
        <Box
            component="ul"
            sx={{
                m: 0,
                pl: 2,
            }}
        >
            {errorMessages.map((errorMessage, index) => (
                <li key={index}>{errorMessage}</li>
            ))}
        </Box>
    ) : errorMessages[0] ?? hint;
    const hasError = errorMessages.length > 0;
    const passThroughSlotProps = muiPassTroughProps?.slotProps;
    const passThroughSx = muiPassTroughProps?.sx;
    const passThroughInputSlotProps = typeof passThroughSlotProps?.input === 'function'
        ? undefined
        : passThroughSlotProps?.input;
    const passThroughHtmlInputSlotProps = typeof passThroughSlotProps?.htmlInput === 'function'
        ? undefined
        : passThroughSlotProps?.htmlInput;

    const handleChange = (newValue: string, info: MuiTelInputInfo) => {
        if (readonly || busy) {
            return;
        }

        lastInputInfoRef.current = info;
        setInputValue(newValue);
        onChange(isBlankPhoneNumber(info.nationalNumber) ? null : cleanPhoneNumberValue(newValue));
    };

    const handleBlur = (
        _event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>,
        info?: MuiTelInputInfo,
    ) => {
        if (info != null) {
            lastInputInfoRef.current = info;
        }

        const canonicalValue = getCanonicalPhoneNumber(inputValue, info ?? lastInputInfoRef.current);

        if (canonicalValue !== cleanPhoneNumberValue(inputValue)) {
            setInputValue(canonicalValue ?? '');
            onChange(canonicalValue);
        }

        onBlur?.(canonicalValue);
    };

    return (
        <FormField
            id={props.id ?? muiPassTroughProps?.id}
            label={label}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            labelAction={props.labelAction}
            hint={!hasError ? helperText : undefined}
            error={hasError ? helperText : undefined}
            required={required}
            disabled={disabled}
            readOnly={readonly}
            busy={busy}
            margin={props.margin ?? muiPassTroughProps?.margin ?? 'normal'}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
        >
            {(fieldContext: FormFieldControlContext) => {
                const nativeAriaProps = getNativeInputAriaProps(fieldContext, passThroughHtmlInputSlotProps);

                return (
                    <MuiTelInput
                        {...muiPassTroughProps}
                        id={fieldContext.controlId}
                        label={undefined}
                        value={inputValue}
                        onChange={handleChange}
                        onBlur={handleBlur}
                        placeholder={placeholder}
                        required={required}
                        disabled={disabled}
                        defaultCountry="DE"
                        preferredCountries={preferredCountries}
                        forceCallingCode
                        disableFormatting
                        disableDropdown={busy}
                        focusOnSelectCountry
                        langOfCountryName="de"
                        fullWidth
                        margin="none"
                        size={size}
                        error={fieldContext.invalid}
                        helperText={undefined}
                        getFlagElement={(isoCode, {countryName}) => CountryCodeFlagElement(isoCode, countryName)}
                        unknownFlagElement={CountryCodeFlagElement('ZZ' as MuiTelInputCountry, 'Unbekannte Ländervorwahl')}
                        slotProps={{
                            input: {
                                ...passThroughInputSlotProps,
                                readOnly: readonly || busy || passThroughInputSlotProps?.readOnly,
                            },
                            htmlInput: {
                                ...passThroughHtmlInputSlotProps,
                                ...nativeAriaProps,
                            },
                        }}
                        sx={[
                            {
                                backgroundColor: busy ? getDisabledFieldBackground : undefined,
                                cursor: busy ? 'not-allowed' : undefined,
                                '& .MuiInputBase-root': {
                                    minHeight: FormFieldTokens.controlMinHeight,
                                },
                            },
                            ...(Array.isArray(passThroughSx) ? passThroughSx : [passThroughSx]),
                            ...(Array.isArray(controlSx) ? controlSx : [controlSx]),
                        ]}
                    />
                );
            }}
        </FormField>
    );
}
