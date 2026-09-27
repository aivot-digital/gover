import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

    it('hides disabled preference choices while keeping the general selection available', async () => {
        const user = userEvent.setup();
        render(
            <AssignmentContextFieldComponent
                value={null}
                onChange={vi.fn()}
                options={[]}
                disableProcessInstanceAssigneeOption
                disableAssignmentContextRepeatExecutionAssigneePreferenceOptions
            />,
        );

        expect(screen.queryByTitle('Bevorzugung bei erneuter Ausführung (Schleife)')).not.toBeInTheDocument();
        expect(screen.queryByText('Wenn dieselbe Aufgabe im Rahmen einer Schleife erneut ausgeführt wird, wird die Bevorzugung bei erneuter Ausführung berücksichtigt.')).not.toBeInTheDocument();

        await user.click(screen.getByRole('combobox', {name: /Bevorzugung bei der Zuweisung/}));
        expect(screen.queryByText('Bevorzuge die dem Vorgang zugewiesene Mitarbeiter:in')).not.toBeInTheDocument();
        expect(screen.getByText('Bevorzuge Bearbeiter:in des vorherigen Prozessschritts')).toBeInTheDocument();
    });

    it('keeps a saved disabled repeat preference hidden without changing it', () => {
        const onChange = vi.fn();
        const selection = [{type: 'user' as const, id: 'recipient'}];
        render(
            <AssignmentContextFieldComponent
                value={{
                    domainAndUserSelection: selection,
                    generalAssigneePreference: 'none',
                    repeatExecutionAssigneePreference: 'previousIterationAssignee',
                }}
                onChange={onChange}
                options={[]}
                disableAssignmentContextRepeatExecutionAssigneePreferenceOptions
            />,
        );

        expect(screen.queryByTitle('Bevorzugung bei erneuter Ausführung (Schleife)')).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Bevorzugung entfernen'})).not.toBeInTheDocument();
        expect(onChange).not.toHaveBeenCalled();
    });

    it('shows a saved disabled general preference as an error until it is changed', () => {
        render(
            <AssignmentContextFieldComponent
                value={{
                    domainAndUserSelection: [{type: 'user', id: 'recipient'}],
                    generalAssigneePreference: 'processInstanceAssignee',
                    repeatExecutionAssigneePreference: 'none',
                }}
                onChange={vi.fn()}
                options={[]}
                disableProcessInstanceAssigneeOption
            />,
        );

        expect(screen.getByText('Die Bevorzugung der dem Vorgang zugewiesenen Person ist hier nicht zulässig.'))
            .toBeInTheDocument();
        expect(screen.getByRole('combobox', {name: /Bevorzugung bei der Zuweisung/})).toHaveAttribute('aria-invalid', 'true');
    });
});
