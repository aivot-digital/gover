import {ElementType} from './element-type';
import { HtmlAutofillAttributeOptions } from '../html-autofill-attribute-options';

export const ElementAutofillMapping: Record<ElementType, string[]> = {
    [ElementType.Root]: [],
    [ElementType.Step]: [],
    [ElementType.Alert]: [],
    [ElementType.Container]: [],
    [ElementType.Checkbox]: [],
    [ElementType.Date]: [
        'bday',
        'bday-year',
    ],
    [ElementType.Headline]: [],
    [ElementType.MultiCheckbox]: [],
    [ElementType.Number]: [],
    [ElementType.ReplicatingContainer]: [],
    [ElementType.Richtext]: [],
    [ElementType.Radio]: [],
    [ElementType.Select]: [
        'honorific-prefix',
        'honorific-suffix',
        'address-level2',
        'address-level1',
        'country',
        'country-name',
        'language',
        'sex',
    ],
    [ElementType.Spacer]: [],
    [ElementType.Table]: [],
    [ElementType.Text]: [
        'honorific-prefix',
        'honorific-suffix',
        'given-name',
        'additional-name',
        'family-name',
        'name',
        'nickname',
        'sex',
        'language',
        'organization-title',
        'username',
        'street-address',
        'address-line1',
        'address-line2',
        'address-line3',
        'address-level4',
        'address-level3',
        'address-level2',
        'address-level1',
        'postal-code',
        'country',
        'country-name',
        'email',
        'url',
        'tel',
        'organization',
    ],
    [ElementType.Time]: [],
    [ElementType.IntroductionStep]: [],
    [ElementType.SubmitStep]: [],
    [ElementType.SummaryStep]: [],
    [ElementType.Image]: [],
    [ElementType.SubmittedStep]: [],
    [ElementType.FileUpload]: [],
};

export const getAutofillOptionsForElementType = (elementType: ElementType) => {
    const allowedValues = ElementAutofillMapping[elementType];
    const allowedOptions = HtmlAutofillAttributeOptions.filter(option => allowedValues.includes(option.value));
    allowedOptions.sort((a, b) => allowedValues.indexOf(a.value) - allowedValues.indexOf(b.value));
    return allowedOptions;
};