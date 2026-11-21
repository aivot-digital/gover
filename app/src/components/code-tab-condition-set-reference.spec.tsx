import {fireEvent, render, screen, within} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {CodeTabConditionSetEditor} from './code-tab-condition-set-editor';
import {ConditionSetOperator} from '../data/condition-set-operator';
import {ConditionOperator} from '../data/condition-operator';
import {ElementType} from '../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ConditionSet} from '../models/functions/conditions/condition-set';
import {SelectFieldElement} from '../models/elements/form/input/select-field-element';
import {LegacyNoCodeReference} from '../modules/nocode/components/legacy-no-code-reference';

const field = {
    ...generateElementWithDefaultValues(ElementType.Text),
    id: 'first',
    label: 'Vorname',
};
const other = {
    ...generateElementWithDefaultValues(ElementType.Text),
    id: 'second',
    label: 'Vorname',
};
const select: SelectFieldElement = {
    ...generateElementWithDefaultValues(ElementType.Select),
    id: 'choice',
    label: 'Auswahl',
    options: [{
        value: 'a',
        label: 'Option A',
    }],
};

function setup(conditionSet: ConditionSet, validation = true) {
    return render(<LegacyNoCodeReference>
        <CodeTabConditionSetEditor element={field}
                                   allElements={[field, other, select].map(element => ({
                                       element,
                                       parents: [],
                                   }))}
                                   conditionSet={conditionSet}
                                   onChange={() => undefined}
                                   editable={false}
                                   shouldReturnString={validation}
                                   presentation="reference"/>
    </LegacyNoCodeReference>);
}

describe('legacy rule reference', () => {
    it('shows nested logic, values and effective messages without disabled form controls', () => {
        const rule: ConditionSet = {
            operator: ConditionSetOperator.All,
            conditionSetUnmetMessage: 'Unbenutzte Gruppenmeldung',
            conditions: [{
                reference: 'first',
                operator: ConditionOperator.Equals,
                value: 'Daniel',
                conditionUnmetMessage: 'Vorname prüfen',
            }],
            conditionsSets: [{
                operator: ConditionSetOperator.Any,
                conditionSetUnmetMessage: 'Gruppe prüfen',
                conditions: [{
                    reference: 'second',
                    operator: ConditionOperator.NotEmpty,
                    conditionUnmetMessage: 'Verdeckte Meldung',
                }],
            }],
        };
        const snapshot = structuredClone(rule);
        setup(rule);
        const reference = screen.getByRole('region', {name: 'Bisherige Regel Nur lesbar'});
        expect(within(reference).queryByRole('textbox')).not.toBeInTheDocument();
        expect(within(reference).queryByRole('combobox')).not.toBeInTheDocument();
        expect(screen.getByText(/wenn nicht alle folgenden Bedingungen/)).toBeVisible();
        expect(screen.getByText(/Mindestens eine dieser Bedingungen muss erfüllt sein \(ODER\)/)).toBeVisible();
        expect(screen.getByText('ID: first')).toBeVisible();
        expect(screen.getByText('Daniel')).toBeVisible();
        expect(screen.getByText(/Vorname prüfen/)).toBeVisible();
        expect(screen.getByText(/Gruppe prüfen/)).toBeVisible();
        expect(screen.getByText(/Verdeckte Meldung/)).not.toBeVisible();
        expect(screen.getByText(/Unbenutzte Gruppenmeldung/)).not.toBeVisible();
        for (const summary of screen.getAllByText('Weitere gespeicherte Angaben')) fireEvent.click(summary);
        expect(screen.getByText(/Verdeckte Meldung/)).toBeVisible();
        expect(screen.getByText(/Unbenutzte Gruppenmeldung/)).toBeVisible();
        expect(rule).toEqual(snapshot);
        fireEvent.click(screen.getByRole('button', {name: 'Bisherige Regel Nur lesbar'}));
        expect(screen.getByRole('button', {name: 'Bisherige Regel Nur lesbar'})).toHaveAttribute('aria-expanded', 'false');
    });

    it('keeps nested AND groups positive inside an inverted validation and suppresses their messages under OR', () => {
        setup({
            operator: ConditionSetOperator.Any,
            conditionSetUnmetMessage: 'Gesamtmeldung',
            conditionsSets: [
                {
                    operator: ConditionSetOperator.All,
                    conditions: [
                        {
                            reference: 'first',
                            operator: ConditionOperator.Equals,
                            value: '',
                            conditionUnmetMessage: 'Untergeordnete Meldung',
                        },
                    ],
                },
            ],
        });
        expect(screen.getByText(/wenn keine der folgenden Bedingungen erfüllt ist/)).toBeVisible();
        expect(screen.getByText(/Alle diese Bedingungen müssen erfüllt sein \(UND\)/)).toBeVisible();
        expect(screen.getByText('Leerer Text ("")')).toBeVisible();
        expect(screen.getByText(/Gesamtmeldung/)).toBeVisible();
        expect(screen.getByText(/Untergeordnete Meldung/)).not.toBeVisible();
    });

    it('distinguishes element targets and option labels from the stored values', () => {
        setup({
            operator: ConditionSetOperator.All,
            conditions: [
                {
                    reference: 'first',
                    operator: ConditionOperator.Equals,
                    target: 'second',
                },
                {
                    reference: 'choice',
                    operator: ConditionOperator.Equals,
                    value: 'a',
                },
                {
                    reference: 'missing',
                    operator: ConditionOperator.Equals,
                    value: 'unverändert',
                },
            ],
        }, false);
        expect(screen.getByText('ID: first')).toBeVisible();
        expect(screen.getByText('ID: second')).toBeVisible();
        expect(screen.getByText('Option A')).toBeVisible();
        expect(screen.getByText('Gespeicherter Wert: a')).toBeVisible();
        expect(screen.getByText('ID: missing')).toBeVisible();
        expect(screen.getAllByRole('term', {name: ''}).map(term => term.textContent)).toEqual([
            'Element', 'Vergleich', 'Vergleichselement',
            'Element', 'Vergleich', 'Wert',
            'Element', 'Vergleich', 'Wert',
        ]);
        const target = screen.getByText('Vergleichselement').nextElementSibling!;
        expect(within(target as HTMLElement).getByText('Vorname')).toBeVisible();
        expect(within(target as HTMLElement).getByText('ID: second')).toBeVisible();
        expect(screen.getByText('Nicht gefundenes Element')).toBeVisible();
        expect(screen.getByText('unverändert')).toBeVisible();
    });

    it('preserves stored messages on visibility rules without presenting them as validation errors', () => {
        setup({
            operator: ConditionSetOperator.Any,
            conditionSetUnmetMessage: 'Gespeichert',
            conditions: [],
        }, false);
        expect(screen.getByText(/Die Regel ist wahr, wenn mindestens eine/)).toBeVisible();
        expect(screen.getByText('Keine Bedingungen hinterlegt.')).toBeVisible();
        expect(screen.getByText(/Gespeichert/)).not.toBeVisible();
        fireEvent.click(screen.getByText('Weitere gespeicherte Angaben'));
        expect(screen.getByText(/Gespeichert/)).toBeVisible();
    });
});
