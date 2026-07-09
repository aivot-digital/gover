import {parsePhoneNumberFromString} from 'libphonenumber-js';

const E164PhoneNumberRegex = /^\+[1-9]\d{1,14}$/;

function parseValidPhoneNumber(value: string): ReturnType<typeof parsePhoneNumberFromString> {
    const phoneNumber = parsePhoneNumberFromString(value);
    if (phoneNumber == null || !phoneNumber.isValid() || phoneNumber.ext != null) {
        return undefined;
    }

    return phoneNumber;
}

export function isBlankPhoneNumber(value: string | null | undefined): boolean {
    return value == null || value.trim().length === 0;
}

export function isValidPhoneNumber(value: string | null | undefined): boolean {
    if (isBlankPhoneNumber(value)) {
        return true;
    }

    return parseValidPhoneNumber(value!.trim()) != null;
}

export function normalizePhoneNumberForTelLink(value: string | null | undefined): string | undefined {
    if (isBlankPhoneNumber(value)) {
        return undefined;
    }

    const trimmedValue = value!.trim();
    const phoneNumber = parseValidPhoneNumber(trimmedValue);
    if (phoneNumber == null) {
        return undefined;
    }

    const normalizedValue = phoneNumber.number;
    return E164PhoneNumberRegex.test(normalizedValue)
        ? normalizedValue
        : undefined;
}

export function formatPhoneNumberForDisplay(value: string | null | undefined): string {
    if (isBlankPhoneNumber(value)) {
        return '';
    }

    const trimmedValue = value!.trim();
    const phoneNumber = parseValidPhoneNumber(trimmedValue);
    if (phoneNumber == null) {
        return trimmedValue;
    }

    if (phoneNumber.country === 'DE') {
        const nationalNumber = phoneNumber.formatNational();
        if (nationalNumber.startsWith('0')) {
            return `+49 (0) ${nationalNumber.slice(1).trim()}`;
        }
    }

    return phoneNumber.formatInternational();
}
