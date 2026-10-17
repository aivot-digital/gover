import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {type DynamicTextPolicy} from '../../../input-mode';

export interface RichTextInputElement extends BaseInputElement<ElementType.RichTextInput> {
    dynamicTextPolicy?: DynamicTextPolicy | null;
    reducedMode: boolean | null | undefined;
}
