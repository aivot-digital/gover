import {Form} from '../models/entities/form';
import {flattenElements} from './flatten-elements';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementType} from '../data/element-type/element-type';

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

export function formToTypeDefinition(form: Form): string {
    const lines = ['interface Data {'];
    for (const element of flattenElements(form.root)) {
        if (isAnyInputElement(element)) {
            lines.push(`    ${element.id}: undefined | null | ${typeMap[element.type]};`);
        }
    }
    lines.push('};');
    return lines.join('\n');
}