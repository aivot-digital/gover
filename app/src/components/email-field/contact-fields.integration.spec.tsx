import {describe, expect, it} from 'vitest';
import * as yup from 'yup';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {prosunaSchemaToYup} from '../../utils/prosuna-schema-to-yup';
import {literalAuthoredValue} from '../../models/element-data';
import {PhoneNumberValidation} from '../../models/elements/form/input/phone-number-field-element';
import {NoCodeDataTypeMap} from '../../modules/nocode/data/no-code-data-type-map';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ElementIsInput} from '../../data/element-type/element-is-input';

function schema(type: ElementType.PhoneNumber | ElementType.Email, options = {}) {
    return yup.object(prosunaSchemaToYup({...generateElementWithDefaultValues(type), id: 'contact', ...options}, {}));
}
const input = (value: unknown) => ({contact: literalAuthoredValue(value)});

describe('contact fields integration', () => {
    it.each([ElementType.PhoneNumber, ElementType.Email] as const)('registers type %s as a string input and enforces required values', async type => {
        expect(ElementIsInput[type]).toBe(true);
        expect(NoCodeDataTypeMap[type]).toBe(NoCodeDataType.String);
        await expect(schema(type).validate(input(null))).resolves.toBeDefined();
        await expect(schema(type, {required: true}).validate(input(' '))).rejects.toThrow('Pflichtfeld');
        await expect(schema(type).validate(input('\u00a0\u202f'))).resolves.toBeDefined();
        await expect(schema(type, {required: true}).validate(input('\u00a0\u202f'))).rejects.toThrow('Pflichtfeld');
    });
    it('defaults to possible phone numbers and preserves strict validation as an option', async () => {
        await expect(schema(ElementType.PhoneNumber).validate(input('+49 1234'))).resolves.toBeDefined();
        await expect(schema(ElementType.PhoneNumber, {validationMode: PhoneNumberValidation.Strict}).validate(input('+49 1234'))).rejects.toThrow();
        await expect(schema(ElementType.PhoneNumber).validate(input('+49 30 123456 ext. 7'))).rejects.toThrow();
        await expect(schema(ElementType.PhoneNumber).validate(input('030 123456'))).rejects.toThrow();
    });
    it('permits local domains only when enabled', async () => {
        await expect(schema(ElementType.Email).validate(input('name@verwaltung'))).rejects.toThrow();
        await expect(schema(ElementType.Email, {allowLocalDomains: true}).validate(input('name@verwaltung'))).resolves.toBeDefined();
        await expect(schema(ElementType.Email).validate(input('name@büro.de'))).resolves.toBeDefined();
    });
});
