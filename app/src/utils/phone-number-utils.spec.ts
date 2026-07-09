import {
    formatPhoneNumberForDisplay,
    isValidPhoneNumber,
    normalizePhoneNumberForTelLink,
} from './phone-number-utils';

describe('phone-number-utils', () => {
    it('normalizes valid phone numbers for tel links', () => {
        expect(normalizePhoneNumberForTelLink('+49 30 123456')).toBe('+4930123456');
    });

    it('formats German phone numbers with optional national prefix marker', () => {
        expect(formatPhoneNumberForDisplay('+4930123456')).toBe('+49 (0) 30 123456');
    });

    it('rejects phone numbers with extensions', () => {
        const phoneNumberWithExtension = '+49 30 123456 ext. 7';

        expect(isValidPhoneNumber(phoneNumberWithExtension)).toBe(false);
        expect(normalizePhoneNumberForTelLink(phoneNumberWithExtension)).toBeUndefined();
        expect(formatPhoneNumberForDisplay(phoneNumberWithExtension)).toBe(phoneNumberWithExtension);
    });
});
