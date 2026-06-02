import {ElementType} from '../data/element-type/element-type';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';

describe('generateElementWithDefaultValues', () => {
    it('should default text fields to not being copyable', () => {
        const element = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        expect(element.copyable).toBe(false);
    });
});
