import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type FormLayoutElement} from '../models/elements/form-layout-element';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {mergeRecalculatedReferencedIds, normalizeUiDefinitionForStorage} from './ui-definition-utils';

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

describe('mergeRecalculatedReferencedIds', () => {
    // The backend serializes absent element properties as null.
    function serializeLikeBackend<T>(value: T): T {
        return {...cloneThroughJson(value), name: null, visibility: null, override: null, metadata: null};
    }

    it('keeps an untouched generated definition when the recalculation only adds null properties', () => {
        const draft = generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout;

        const merged = mergeRecalculatedReferencedIds(draft, serializeLikeBackend(draft));

        expect(merged).toStrictEqual(draft);
        expect(normalizeUiDefinitionForStorage(merged)).toBeNull();
    });

    it('applies recalculated referenced IDs of nested functions without adopting other changes', () => {
        const model: TextFieldElement = {
            ...(generateElementWithDefaultValues(ElementType.Text) as TextFieldElement),
            id: 'model',
            label: 'Modell',
            visibility: {
                type: 'NoCode',
                requirements: undefined,
                conditionSet: undefined,
                noCode: {type: 'NoCodeProcessDataReference', path: 'fahrzeug.marke'},
                javascriptCode: undefined,
                referencedIds: [],
            },
            value: {
                type: 'NoCode',
                requirements: undefined,
                noCode: {type: 'NoCodeProcessDataReference', path: 'fahrzeug'},
                javascriptCode: undefined,
                referencedIds: [],
            },
        };
        const draft: GroupLayout = {
            ...(generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout),
            id: 'root',
            children: [{
                ...(generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout),
                id: 'section',
                children: [model],
            }],
        };
        const recalculated = serializeLikeBackend(draft);
        const recalculatedSection = recalculated.children[0] as GroupLayout;
        recalculatedSection.children = [{
            ...(recalculatedSection.children[0] as TextFieldElement),
            label: 'Vom Server geändert',
            visibility: {...model.visibility!, referencedIds: ['brand']},
            value: {...model.value!, referencedIds: ['brand', 'color']},
        }];

        const merged = mergeRecalculatedReferencedIds(draft, recalculated);

        expect(merged).toStrictEqual({
            ...draft,
            children: [{
                ...draft.children[0],
                children: [{
                    ...model,
                    visibility: {...model.visibility, referencedIds: ['brand']},
                    value: {...model.value, referencedIds: ['brand', 'color']},
                }],
            }],
        });
        expect(model.visibility?.referencedIds).toEqual([]);
    });
});
