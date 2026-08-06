import {ElementType} from '../data/element-type/element-type';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {cloneElement} from './clone-element';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {describe, expect, it} from 'vitest';

describe('cloneElement', () => {
    it('should append the copy suffix only to the cloned root element', () => {
        const textField = {
            ...generateElementWithDefaultValues(ElementType.Text) as TextFieldElement,
            id: 'child-field',
            name: undefined,
        };
        const nestedGroup = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
            id: 'nested-group',
            name: 'Nested group',
            children: [
                textField,
            ],
        };
        const group = {
            ...generateElementWithDefaultValues(ElementType.GroupLayout) as GroupLayout,
            id: 'parent-group',
            name: 'Parent group',
            children: [
                nestedGroup,
            ],
        };

        const clone = cloneElement(group);
        const clonedNestedGroup = clone.children[0] as GroupLayout;

        expect(clone.name).toBe('Parent group (Kopie)');
        expect(clonedNestedGroup.name).toBe('Nested group');
        expect(clonedNestedGroup.children[0].name).toBeUndefined();
    });
});
