import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {ConditionOperator} from '../data/condition-operator';
import {ElementType} from '../data/element-type/element-type';
import {OptionsSourceType} from '../models/elements/form/input/options-source-type';
import {BaseApiService} from '../services/base-api-service';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {CodeTabCondition} from './code-tab-condition';

describe('CodeTabCondition', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('uses codelist options for legacy no-code values', async () => {
        vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue([
            {label: 'Berlin', value: 'BE'},
            {label: 'Hamburg', value: 'HH'},
        ]);
        const onChange = vi.fn();
        const element = {
            ...generateElementWithDefaultValues(ElementType.Select),
            id: 'state',
            label: 'Bundesland',
            optionsSource: OptionsSourceType.CodeList,
            codeListKey: 'states',
            options: [{label: 'Veraltet', value: 'old'}],
        };

        render(
            <CodeTabCondition
                allElements={[{element, parents: []}]}
                cond={{
                    reference: element.id,
                    operator: ConditionOperator.Equals,
                    value: '',
                }}
                index={0}
                onDelete={vi.fn()}
                onChange={onChange}
                editable
            />,
        );

        const valueSelect = await screen.findByRole('combobox', {name: 'Wert'});
        fireEvent.mouseDown(valueSelect);
        const option = await screen.findByRole('option', {name: 'Hamburg'});
        fireEvent.click(option);

        expect(onChange).toHaveBeenLastCalledWith({
            reference: element.id,
            operator: ConditionOperator.Equals,
            value: 'HH',
        });
        await waitFor(() => expect(screen.queryByRole('option', {name: 'Veraltet'})).not.toBeInTheDocument());
    });
});
