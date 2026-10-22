import {describe, expect, it} from 'vitest';
import cases from '../../../backend/src/test/resources/email-address-cases.json';
import {normalizeEmailAddress, validateEmailAddress} from './email-address-utils';

describe('email address contract shared with the backend', () => {
    it.each(cases)('$value (local=$allowLocalDomains)', ({value, allowLocalDomains, normalized}) => {
        expect(normalizeEmailAddress(value, allowLocalDomains) ?? null).toBe(normalized);
        expect(validateEmailAddress(value, allowLocalDomains) == null).toBe(normalized != null);
    });
    it('leaves required validation to the field', () => {
        for (const value of [null, undefined, '', ' ']) expect(validateEmailAddress(value)).toBeUndefined();
    });
});
