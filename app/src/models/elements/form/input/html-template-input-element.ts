import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyElement} from '../../any-element';

export interface HtmlTemplateInputElement extends BaseInputElement<ElementType.HtmlTemplateInput> {
}

export interface HtmlTemplateInputValue {
    assetKey: string | null;
    slots: Record<string, string | null> | null;
}

export function isHtmlTemplateInputElement(
    element: AnyElement
): element is HtmlTemplateInputElement {
    return element.type === ElementType.HtmlTemplateInput;
}
