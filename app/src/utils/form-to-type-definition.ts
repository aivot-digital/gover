import {flattenElements} from './flatten-elements';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementType} from '../data/element-type/element-type';
import {FormVersionEntity} from '../modules/forms/entities/form-version-entity';

const typeMap: Record<ElementType, string> = {
    [ElementType.FormLayout]: 'undefined',
    [ElementType.Step]: 'undefined',
    [ElementType.Alert]: 'undefined',
    [ElementType.GroupLayout]: 'undefined',
    [ElementType.Checkbox]: 'boolean',
    [ElementType.Date]: 'string',
    [ElementType.Headline]: 'undefined',
    [ElementType.MultiCheckbox]: 'string[]',
    [ElementType.Number]: 'number',
    [ElementType.ReplicatingContainer]: 'string[]',
    [ElementType.RichText]: 'undefined',
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
    [ElementType.DialogLayout]: 'undefined',
    [ElementType.StepperLayout]: 'undefined',
    [ElementType.ConfigLayout]: 'undefined',
    [ElementType.FunctionInput]: 'undefined',
    [ElementType.CodeInput]: 'string',
    [ElementType.RichTextInput]: 'string',
    [ElementType.UiDefinitionInput]: 'undefined',
    [ElementType.IdentityInput]: 'undefined',
    [ElementType.TabLayout]: 'undefined',
    [ElementType.ChipInput]: 'string[]',
    [ElementType.DateTime]: 'string',
    [ElementType.DateRange]: '{start: string | null | undefined; end: string | null | undefined;}',
    [ElementType.TimeRange]: '{start: string | null | undefined; end: string | null | undefined;}',
    [ElementType.DateTimeRange]: '{start: string | null | undefined; end: string | null | undefined;}',
    [ElementType.MapPoint]: '{latitude: number | null | undefined; longitude: number | null | undefined; address: string | null | undefined;}',
    [ElementType.DomainAndUserSelect]: '{type: \'orgUnit\' | \'team\' | \'user\'; id: string;}[]',
    [ElementType.AssignmentContext]: '{domainAndUserSelection: {type: \'orgUnit\' | \'team\' | \'user\'; id: string;}[] | null | undefined; preferPreviousTaskAssignee: boolean | null | undefined; preferUninvolvedUser: boolean | null | undefined; preferProcessInstanceAssignee: boolean | null | undefined;}',
    [ElementType.DataModelSelect]: 'string',
    [ElementType.DataObjectSelect]: 'string',
    [ElementType.NoCodeInput]: '{noCode: Record<string, unknown> | null;}',
};

export function formToTypeDefinition(form: FormVersionEntity): string {
    const lines = ['interface Data {'];
    for (const element of flattenElements(form.rootElement)) {
        if (isAnyInputElement(element)) {
            lines.push(`    ${element.id}: undefined | null | ${typeMap[element.type]};`);
        }
    }
    lines.push('};');
    return lines.join('\n');
}
