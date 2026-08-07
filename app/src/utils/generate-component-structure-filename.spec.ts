import {describe, expect, it} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {type StepElement} from '../models/elements/steps/step-element';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {generateComponentStructureFilename} from './generate-component-structure-filename';

describe('generateComponentStructureFilename', () => {
    it('should use the element id when a text input has no name or label', () => {
        const element = {
            type: ElementType.Text,
            id: 'text-input-id',
            name: undefined,
            label: undefined,
        } as TextFieldElement;

        expect(generateComponentStructureFilename(element)).toBe('text-input-id.uielement.prosuna.json');
    });

    it('should keep using the label when a text input has one', () => {
        const element = {
            type: ElementType.Text,
            id: 'text-input-id',
            name: undefined,
            label: 'Applicant name',
        } as TextFieldElement;

        expect(generateComponentStructureFilename(element)).toBe('Applicant name.uielement.prosuna.json');
    });

    it('should keep using the internal name when one is configured', () => {
        const element = {
            type: ElementType.Text,
            id: 'text-input-id',
            name: 'Internal applicant name',
            label: undefined,
        } as TextFieldElement;

        expect(generateComponentStructureFilename(element)).toBe('Internal applicant name.uielement.prosuna.json');
    });

    it('should use the element id instead of generic section fallback text', () => {
        const element = {
            type: ElementType.Step,
            id: 'step-id',
            name: undefined,
            title: undefined,
        } as StepElement;

        expect(generateComponentStructureFilename(element)).toBe('step-id.uielement.prosuna.json');
    });
});
