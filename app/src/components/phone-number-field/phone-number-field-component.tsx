import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box} from '@mui/material';
import {MuiTelInput, type MuiTelInputCountry} from 'mui-tel-input';
import {AsYouType} from 'libphonenumber-js/max';
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

    const trimmedValue = value!.trim();
    const parser = new AsYouType();
    parser.input(trimmedValue);
    const callingCode = parser.getCallingCode();

    // A calling code alone is an editing preference, not a telephone number.
    return callingCode != null && trimmedValue.replace(/\s/g, '') === `+${callingCode}` ? null : trimmedValue;
}

function getCanonicalPhoneNumber(value: string): string | null {
    // Canonicalize plausible input; form-level validation decides whether the value is accepted.
    const normalizedValue = normalizePhoneNumber(value);

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
    const lastPropagatedValueRef = useRef(value?.trim() || null);

    useEffect(() => {
        const nextValue = value?.trim() || null;
        // Keep the selected calling code when the parent echoes our empty value (null or '').
        if (nextValue !== lastPropagatedValueRef.current) {
            lastPropagatedValueRef.current = nextValue;
            setInputValue(value ?? '');
        }
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

    const handleChange = (newValue: string) => {
        if (readonly || busy) {
            return;
        }

        // With disableFormatting, country changes can carry stale number metadata.
        const nextValue = cleanPhoneNumberValue(newValue);
        setInputValue(newValue);
        lastPropagatedValueRef.current = nextValue;
        onChange(nextValue);
    };

    const handleBlur = () => {
        const canonicalValue = getCanonicalPhoneNumber(inputValue);

        if (canonicalValue !== cleanPhoneNumberValue(inputValue)) {
            setInputValue(canonicalValue ?? '');
            lastPropagatedValueRef.current = canonicalValue;
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
            externalAction={props.externalAction}
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
