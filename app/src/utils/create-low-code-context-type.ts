import {createLowCodeType} from './create-low-code-type';
import {ElementType} from '../data/element-type/element-type';
import {flattenElements} from './flatten-elements';
import {AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';


export function createLowCodeContextType(element: AnyElement | undefined, rootElement: AnyElement) {
    const formType = createLowCodeType('Form', {
        id: 'string',
    });

    const allElements = flattenElements(rootElement, true)
        .filter(elem => isAnyInputElement(elem));

    const inputValuesFields: Record<string, string> = {};
    const computedValuesFields: Record<string, string> = {};
    const valuesFields: Record<string, string> = {};
    const visibilitiesFields: Record<string, string> = {};
    const errorsFields: Record<string, string> = {};
    const overridesFields: Record<string, string> = {};

    for (const element of allElements) {
        inputValuesFields[element.id] = `undefined | null | ${typeMap[element.type]}`;
        computedValuesFields[element.id] = `undefined | null | ${typeMap[element.type]}`;
        valuesFields[element.id] = `undefined | null | ${typeMap[element.type]}`;
        visibilitiesFields[element.id] = 'boolean';
        errorsFields[element.id] = 'undefined | null | string';
        overridesFields[element.id] = 'undefined | null | ' + elementToTypeDefinition(element);
    }

    const inputValuesType = createLowCodeType('InputValues', inputValuesFields);
    const computedValuesType = createLowCodeType('ComputedValues', computedValuesFields);
    const valuesType = createLowCodeType('Values', valuesFields);
    const visibilitiesType = createLowCodeType('Visibilities', visibilitiesFields);
    const errorsType = createLowCodeType('Errors', errorsFields);
    const overridesType = createLowCodeType('Overrides', overridesFields);

    const lowCodePrepared: Record<string, any> = {
        id: 'string',
        inputValues: 'InputValues',
        computedValues: 'ComputedValues',
        values: 'Values',
        visibilities: 'Visibilities',
        errors: 'Errors',
        overrides: 'Overrides',
    }

    if (element != null) {
        lowCodePrepared['element'] = elementToTypeDefinition(element);
    }

    const contextType = createLowCodeType('ctx', lowCodePrepared);

    return `// TYPES
    declare ${formType}
    declare ${inputValuesType}
    declare ${computedValuesType}
    declare ${valuesType}
    declare ${visibilitiesType}
    declare ${errorsType}
    declare ${overridesType}
    
    // CONTEXT
    declare ${contextType}
    `;
}

function elementToTypeDefinition(element: AnyElement): string {
    const lines = ['{'];
    for (const e of Object.keys(element)) {
        lines.push(`        ${e}: ${typeof [element.type]};`);
    }
    lines.push('    }');
    return lines.join('\n');
}

const typeMap: Record<ElementType, string> = {
    [ElementType.Root]: 'undefined',
    [ElementType.Step]: 'undefined',
    [ElementType.Alert]: 'undefined',
    [ElementType.Container]: 'undefined',
    [ElementType.Checkbox]: 'boolean',
    [ElementType.Date]: 'string',
    [ElementType.Headline]: 'undefined',
    [ElementType.MultiCheckbox]: 'string[]',
    [ElementType.Number]: 'number',
    [ElementType.ReplicatingContainer]: 'string[]',
    [ElementType.Richtext]: 'undefined',
    [ElementType.Radio]: 'string',
    [ElementType.Select]: 'string',
    [ElementType.Spacer]: 'undefined',
    [ElementType.Table]: 'Record<string, string | number>[]',
    [ElementType.Text]: 'string',
    [ElementType.Time]: 'string',
    [ElementType.IntroductionStep]: 'undefined',
    [ElementType.SubmitStep]: 'undefined',
    [ElementType.SummaryStep]: 'undefined',
    [ElementType.Image]: 'undefined',
    [ElementType.SubmittedStep]: 'undefined',
    [ElementType.FileUpload]: '{name: string; uri: string; size: number;}',
};