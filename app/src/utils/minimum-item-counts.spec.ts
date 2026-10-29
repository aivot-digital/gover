import {describe, expect, it} from 'vitest';
import * as yup from 'yup';
import {ElementType} from '../data/element-type/element-type';
import {literalAuthoredValue} from '../models/element-data';
import {MultiChoicePresentation} from '../models/elements/form/input/multi-choice-presentation';
import {generateElementWithDefaultValues} from './generate-element-with-default-values';
import {prosunaSchemaToYup} from './prosuna-schema-to-yup';

const cases = [
    {type: ElementType.ChipInput, minimum: 'minItems', maximum: 'maxItems', options: {}, item: (i: number) => `item-${i}`},
    ...[MultiChoicePresentation.Checkboxes, MultiChoicePresentation.Combobox].map(presentation => ({
        type: ElementType.MultiCheckbox as const, minimum: 'minimumRequiredOptions', maximum: undefined,
        options: {presentation}, item: (i: number) => `item-${i}`,
    })),
    {type: ElementType.Table, minimum: 'minimumRequiredRows', maximum: 'maximumRows', options: {}, item: (i: number) => ({name: `item-${i}`})},
    {type: ElementType.ReplicatingContainer, minimum: 'minimumRequiredSets', maximum: 'maximumSets', options: {}, item: (i: number) => ({id: `row-${i}`, values: {}})},
    {type: ElementType.FileUpload, minimum: 'minFiles', maximum: 'maxFiles', options: {isMultifile: true}, item: (i: number) => ({name: `${i}.pdf`, uri: `file-${i}`, size: 1})},
    {type: ElementType.ProcessInstanceAttachmentSetSelect, minimum: 'minItems', maximum: 'maxItems', options: {}, item: (i: number) => `set-${i}`},
    {type: ElementType.DomainAndUserSelect, minimum: 'minItems', maximum: 'maxItems', options: {}, item: (i: number) => ({type: 'user', id: `${i}`})},
    {type: ElementType.AssignmentContext, minimum: 'minItems', maximum: 'maxItems', options: {}, item: (i: number) => ({type: 'user', id: `${i}`})},
] as const;

describe.each(cases)('item count validation: $type $options', testCase => {
    function schema(required: boolean, minimum: number | null = 3, maximum?: number) {
        const element = {
            ...generateElementWithDefaultValues(testCase.type), ...testCase.options,
            id: 'items', required, [testCase.minimum]: minimum,
            ...(testCase.maximum ? {[testCase.maximum]: maximum} : {}),
        };
        return yup.object(prosunaSchemaToYup(element, {}));
    }

    function entries(count: number) {
        const items = Array.from({length: count}, (_, i) => testCase.item(i));
        return testCase.type === ElementType.AssignmentContext ? {domainAndUserSelection: items} : items;
    }

    it('allows optional missing, null and empty values despite a minimum', async () => {
        await expect(schema(false).validate({})).resolves.toBeDefined();
        for (const value of [null, entries(0)]) {
            await expect(schema(false).validate({items: literalAuthoredValue(value)})).resolves.toBeDefined();
        }
    });

    it.each([false, true])('enforces the minimum once entries exist (required=%s)', async required => {
        await expect(schema(required).validate({items: literalAuthoredValue(entries(1))})).rejects.toThrow('Mindestens 3');
        await expect(schema(required).validate({items: literalAuthoredValue(entries(3))})).resolves.toBeDefined();
    });

    it.each([null, 0, 3])('enforces required independently of minimum=%s', async minimum => {
        await expect(schema(true, minimum).validate({})).rejects.toThrow('Pflichtfeld');
        for (const value of [null, entries(0)]) {
            await expect(schema(true, minimum).validate({items: literalAuthoredValue(value)})).rejects.toThrow('Pflichtfeld');
        }
        if (!minimum) {
            await expect(schema(true, minimum).validate({items: literalAuthoredValue(entries(1))})).resolves.toBeDefined();
        }
    });

    if (testCase.maximum) {
        it('preserves maximum limits and accepts zero as unlimited', async () => {
            await expect(schema(false, 3, 3).validate({items: literalAuthoredValue(entries(4))})).rejects.toThrow('Maximal 3');
            await expect(schema(false, 0, 0).validate({items: literalAuthoredValue(entries(4))})).resolves.toBeDefined();
        });
    }
});

it('ignores multifile minimums in single-file mode', async () => {
    const element = {...generateElementWithDefaultValues(ElementType.FileUpload), id: 'files', minFiles: 3, isMultifile: false};
    const schema = yup.object(prosunaSchemaToYup(element, {}));
    await expect(schema.validate({files: literalAuthoredValue([{name: 'a.pdf', uri: 'a', size: 1}])})).resolves.toBeDefined();
});
