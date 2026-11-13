import {describe, expect, it, vi} from 'vitest';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {PhoneNumberFieldEditor} from './phone-number-field-editor';
import {EmailFieldEditor} from './email-field-editor';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';
import {PhoneNumberValidation} from '../models/elements/form/input/phone-number-field-element';

const common = {editable: true, hasSummaryLayoutParent: false, scope: 'application' as const};

describe('contact field editors', () => {
    it('defaults to plausibility and explains the stricter alternative', async () => {
        const element = generateElementWithDefaultValues(ElementType.PhoneNumber);
        const onPatch = vi.fn();
        const {rerender} = render(<PhoneNumberFieldEditor {...common} element={element} onPatch={onPatch} />);
        expect(screen.getByRole('combobox', {name: 'Prüfung'})).toHaveTextContent('Plausibilitätsprüfung');
        expect(screen.getByRole('combobox', {name: 'Prüfung'})).toHaveAccessibleDescription('Ob die Telefonnummer vergeben oder erreichbar ist, kann nicht geprüft werden.');
        expect(screen.getByText(/Direkt wählbare Durchwahlen/)).toBeVisible();
        const user = userEvent.setup();
        await user.click(screen.getByRole('combobox', {name: 'Prüfung'}));
        expect(screen.getByRole('option', {name: /^Plausibilitätsprüfung/})).toHaveTextContent('Prüft, ob die Nummernlänge für das gewählte Land möglich ist. Für die meisten Formulare empfohlen.');
        const strictOption = screen.getByRole('option', {name: /^Nummernplanprüfung/});
        expect(strictOption).toHaveTextContent('Prüft zusätzlich bekannte Nummernbereiche und Nummernstrukturen.');
        expect(strictOption).toHaveTextContent('Kann gültige Nummern ablehnen, wenn neue Nummernbereiche noch nicht in den Prüfdaten enthalten sind.');
        await user.click(strictOption);
        expect(onPatch).toHaveBeenLastCalledWith({validationMode: PhoneNumberValidation.Strict});
        rerender(<PhoneNumberFieldEditor {...common} element={{...element, validationMode: PhoneNumberValidation.Strict}} onPatch={onPatch} />);
        expect(screen.getByRole('combobox', {name: 'Prüfung'})).toHaveAccessibleDescription('Ob die Telefonnummer vergeben oder erreichbar ist, kann nicht geprüft werden.');
    });
    it('makes the local-domain exception explicit', async () => {
        const onPatch = vi.fn();
        render(<EmailFieldEditor {...common} element={generateElementWithDefaultValues(ElementType.Email)} onPatch={onPatch} />);
        const checkbox = screen.getByRole('checkbox', {name: 'Lokale Domains zulassen'});
        expect(checkbox).not.toBeChecked();
        expect(checkbox).toHaveAccessibleDescription(/Mail-Anbieter/);
        await userEvent.setup().click(checkbox);
        expect(onPatch).toHaveBeenLastCalledWith({allowLocalDomains: true});
    });
});
