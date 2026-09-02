import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {AssignmentContextFieldComponent} from './assignment-context-field-component';
import type {DomainAndUserSelectOption} from '../domain-user-select-field/domain-user-select-options';

const options: DomainAndUserSelectOption[] = [
    {
        key: 'orgUnit:10',
        value: {type: 'orgUnit', id: '10'},
        label: 'Fachbereich Anträge',
        subLabel: 'Organisationseinheit',
        group: 'Organisationseinheiten',
        eligibleUserCount: 0,
    },
];

describe('AssignmentContextFieldComponent', () => {
    it('keeps the section title separate from the required person selection', () => {
        render(
            <AssignmentContextFieldComponent
                value={{
                    domainAndUserSelection: [{type: 'orgUnit', id: '10'}],
                    generalAssigneePreference: 'none',
                    repeatExecutionAssigneePreference: 'none',
                }}
                onChange={vi.fn()}
                options={options}
                required
            />,
        );

        const title = screen.getByText('Verantwortlicher Personenkreis');
        const group = title.parentElement;
        expect(group).toHaveAttribute('role', 'group');
        expect(group).toHaveAttribute('aria-labelledby', title.id);
        const personLabel = screen.getByTitle('Personenkreis');
        const personInput = document.getElementById(personLabel.getAttribute('for')!);
        expect(personInput).toBeRequired();

        expect(screen.getByTitle('Bevorzugung bei der Zuweisung'))
            .toHaveTextContent(/Bevorzugung bei der Zuweisung– optional$/);
        expect(screen.getByTitle('Bevorzugung bei erneuter Ausführung (Schleife)'))
            .toHaveTextContent(/Bevorzugung bei erneuter Ausführung \(Schleife\)– optional$/);
    });

    it('associates the eligibility warning with the affected selection', () => {
        render(
            <AssignmentContextFieldComponent
                value={{
                    domainAndUserSelection: [{type: 'orgUnit', id: '10'}],
                    generalAssigneePreference: 'none',
                    repeatExecutionAssigneePreference: 'none',
                }}
                onChange={vi.fn()}
                options={options}
            />,
        );

        const input = screen.getByRole('combobox', {name: 'Personenkreis – optional'});
        expect(input).toHaveAccessibleDescription(/derzeit keine Person mit dem Recht „Aufgaben bearbeiten“/);
        expect(screen.getByRole('status')).toHaveTextContent('Hinweis zur aktuellen Zuweisung');
    });
});
