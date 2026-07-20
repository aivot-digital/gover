import {ElementType} from '../data/element-type/element-type';
import {type TextFieldElement} from '../models/elements/form/input/text-field-element';
import {type FileUploadElement} from '../models/elements/form/input/file-upload-element';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {ProcessAttachmentDisplayElement} from '../models/elements/form/content/process-attachment-display-element';

describe('generateElementWithDefaultValues', () => {
    it('should default text fields to not being copyable', () => {
        const element = generateElementWithDefaultValues(ElementType.Text) as TextFieldElement;

        expect(element.copyable).toBe(false);
        expect(element.copyValueTemplate).toBeUndefined();
    });

    it('should initialize process attachment display elements without a configured attachment set key', () => {
        const element = generateElementWithDefaultValues(ElementType.ProcessAttachmentDisplay) as ProcessAttachmentDisplayElement;

        expect(element.attachmentSetKey).toBeUndefined();
        expect(element.label).toBeUndefined();
        expect(element.hint).toBeUndefined();
    });

    it('should initialize file upload elements without a submitted file name override', () => {
        const element = generateElementWithDefaultValues(ElementType.FileUpload) as FileUploadElement;

        expect(element.submittedFileName).toBeUndefined();
    });
});
