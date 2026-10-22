import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {type AnyElement} from '../models/elements/any-element';
import {normalizeElementDefinition} from './normalize-element-definition';
import {normalizeUiDefinitionForStorage} from './ui-definition-utils';

describe('legacy radio definitions', () => {
    it('normalizes nested radios without modifying values, functions, or the original definition', () => {
        const legacy = {
            type: 11, id: 'choice', destinationKey: 'contact', displayInline: true, toggleButtons: true,
            options: [{value: 'email', label: 'E-Mail'}],
            value: {type: 'low-code', referencedIds: ['other'], expression: 'source'},
        };
        const definition = {type: ElementType.GroupLayout, id: 'root', children: [
            {type: ElementType.ReplicatingContainer, id: 'rows', children: [legacy]},
        ]} as unknown as AnyElement;
        const normalized = normalizeElementDefinition(definition);
        expect(normalized).toEqual({type: ElementType.GroupLayout, id: 'root', children: [
            {type: ElementType.ReplicatingContainer, id: 'rows', children: [
                {...legacy, type: ElementType.Select, presentation: 'radio'},
            ]},
        ]});
        expect(legacy.type).toBe(11);
        expect(normalizeElementDefinition(normalized)).toBe(normalized);
        expect(normalizeUiDefinitionForStorage(definition)).toEqual(normalized);
    });

    it('does not rewrite payloads or functions that happen to contain a type property', () => {
        const definition = {type: ElementType.Text, id: 'text',
            value: {type: 11, id: 'domain-value'}, options: [{type: 11, id: 'option'}]} as unknown as AnyElement;
        expect(normalizeElementDefinition(definition)).toBe(definition);
    });

    it('leaves existing selects and missing definitions unchanged', () => {
        const definition = {type: ElementType.Select, id: 'existing'} as AnyElement;
        expect(normalizeElementDefinition(definition)).toBe(definition);
        expect(normalizeElementDefinition(null)).toBeNull();
        expect(normalizeElementDefinition(undefined)).toBeUndefined();
    });

    it('updates old UI-definition root constraints', () => {
        const definition = {type: ElementType.UiDefinitionInput, id: 'layout', elementType: 11} as unknown as AnyElement;
        expect(normalizeElementDefinition(definition)).toEqual({...definition, elementType: ElementType.Select});
    });
});
