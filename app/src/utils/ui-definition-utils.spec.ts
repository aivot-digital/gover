import {ElementType} from '../data/element-type/element-type';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type FormLayoutElement} from '../models/elements/form-layout-element';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {normalizeUiDefinitionForStorage} from './ui-definition-utils';

function cloneThroughJson<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
}

describe('normalizeUiDefinitionForStorage', () => {
    it('normalizes untouched empty generated definitions to null', () => {
        const value = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;

        expect(normalizeUiDefinitionForStorage(value)).toBeNull();
    });

    it('normalizes JSON-cloned empty generated form layouts to null', () => {
        const value = cloneThroughJson(generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement);

        expect(normalizeUiDefinitionForStorage(value)).toBeNull();
    });

    it('normalizes JSON-cloned empty generated grouped UI definitions to null', () => {
        const value = cloneThroughJson(generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout);

        expect(normalizeUiDefinitionForStorage(value)).toBeNull();
    });

    it('preserves empty form layouts with changed root settings', () => {
        const value = {
            ...(generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement),
            publicTitle: 'Geänderter Titel',
        };

        expect(normalizeUiDefinitionForStorage(value)).toEqual(value);
    });

    it('preserves empty grouped UI definitions with changed root settings', () => {
        const value = {
            ...(generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout),
            name: 'Geänderte Gruppe',
        };

        expect(normalizeUiDefinitionForStorage(value)).toEqual(value);
    });
});
