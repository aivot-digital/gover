import {describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/react';
import * as yup from 'yup';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {prosunaSchemaToYup} from '../../utils/prosuna-schema-to-yup';
import {literalAuthoredValue} from '../../models/element-data';
import {DurationLargestUnit, DurationPrecision} from '../../models/elements/form/input/duration-field-element';
import {DurationFieldSummary} from '../../summaries/duration-field-summary';
import {NoCodeDataTypeMap, elementMatchesDesiredNoCodeDataType} from '../../modules/nocode/data/no-code-data-type-map';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ElementIsInput} from '../../data/element-type/element-is-input';

function durationSchema(required = false, precision = DurationPrecision.Minute) {
    const field = {...generateElementWithDefaultValues(ElementType.Duration), id: 'duration', required, precision};
    const root = {...generateElementWithDefaultValues(ElementType.GroupLayout), children: [field]};
    return yup.object(prosunaSchemaToYup(root, {}));
}

describe('duration integration', () => {
    it('creates a registered numeric input with minute precision', () => {
        const field = generateElementWithDefaultValues(ElementType.Duration);
        expect(field.label).toBe('Zeitdauer');
        expect(field.precision).toBe(DurationPrecision.Minute);
        expect(field.largestUnit).toBe(DurationLargestUnit.Hour);
        expect(ElementIsInput[field.type]).toBe(true);
        expect(NoCodeDataTypeMap[field.type]).toBe(NoCodeDataType.Number);
        expect(elementMatchesDesiredNoCodeDataType(field.type, NoCodeDataType.Number)).toBe(true);
    });

    it('validates literal wrappers without equating empty and zero', async () => {
        await expect(durationSchema().validate({duration: literalAuthoredValue(null)})).resolves.toBeDefined();
        await expect(durationSchema(true).validate({duration: literalAuthoredValue(null)})).rejects.toThrow('Pflichtfeld');
        await expect(durationSchema(true).validate({duration: literalAuthoredValue(0)})).resolves.toBeDefined();
        await expect(durationSchema().validate({duration: literalAuthoredValue(75)})).rejects.toThrow('ganzen Minuten');
        await expect(durationSchema(false, DurationPrecision.Second).validate({duration: literalAuthoredValue(75)})).resolves.toBeDefined();
        for (const invalid of [-1, 0.5, '', '9000']) {
            await expect(durationSchema().validate({duration: literalAuthoredValue(invalid)})).rejects.toThrow();
        }
    });

    it('uses duration labels in summaries rather than raw seconds', () => {
        const model = generateElementWithDefaultValues(ElementType.Duration);
        const props = {model, authoredElementValues: {}, derivedData: {effectiveValues: {}, elementStates: {}}};
        const {rerender} = render(<DurationFieldSummary {...props} value={99000}/>);
        expect(screen.getByText('27 Stunden 30 Minuten')).toBeVisible();
        rerender(<DurationFieldSummary {...props} model={{...model, largestUnit: DurationLargestUnit.Day}} value={99000}/>);
        expect(screen.getByText('1 Tag 3 Stunden 30 Minuten')).toBeVisible();
        rerender(<DurationFieldSummary {...props} value={0}/>);
        expect(screen.getByText('0 Minuten')).toBeVisible();
    });
});
