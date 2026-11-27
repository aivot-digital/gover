import {describe, expect, it} from 'vitest';
import {createStorageProviderDefinitionOption} from './storage-provider-definition-utils';

describe('createStorageProviderDefinitionOption', () => {
    it('uses the plain-text abstract as the compact option text', () => {
        const result = createStorageProviderDefinitionOption({
            key: 'de.aivot.test.storage',
            version: 1,
            name: 'Test storage',
            abstractDescription: 'Concise **plain-text** abstract.',
            description: 'Detailed **Markdown** description.',
            documentationUrl: null,
            providerConfigLayout: null,
            supportsMetadataAttributes: false,
        });

        expect(result).toEqual({
            value: 'de.aivot.test.storage',
            label: 'Test storage',
            subLabel: 'Concise **plain-text** abstract.',
        });
    });
});
