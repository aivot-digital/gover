import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box} from '@mui/material';
import {MuiTelInput, type MuiTelInputCountry, type MuiTelInputInfo} from 'mui-tel-input';
import {PhoneNumberFieldComponentProps} from './phone-number-field-component-props';
import {isBlankPhoneNumber, normalizePhoneNumberForTelLink} from '../../utils/phone-number-utils';

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
    const normalizedHrefValue = normalizePhoneNumberForTelLink(normalizedInfoValue ?? value);

    return normalizedHrefValue ?? cleanPhoneNumberValue(value);
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
        sx,
        size = 'medium',
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
        <MuiTelInput
            {...muiPassTroughProps}
            label={label}
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
            focusOnSelectCountry
            langOfCountryName="de"
            fullWidth
            size={size}
            error={errorMessages.length > 0}
            helperText={helperText}
            getFlagElement={(isoCode, {countryName}) => CountryCodeFlagElement(isoCode, countryName)}
            unknownFlagElement={CountryCodeFlagElement('ZZ' as MuiTelInputCountry, 'Unbekannte Ländervorwahl')}
            slotProps={{
                input: {
                    readOnly: readonly || busy,
                },
                inputLabel: {
                    title: label,
                },
                htmlInput: {
                    'aria-disabled': busy || disabled,
                },
                formHelperText: {
                    component: 'div',
                },
            }}
            sx={{
                ...sx,
                backgroundColor: busy ? '#F8F8F8' : undefined,
                cursor: busy ? 'not-allowed' : undefined,
            }}
        />
    );
}
