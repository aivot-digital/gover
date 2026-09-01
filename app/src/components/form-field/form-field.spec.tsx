import {describe, expect, it} from 'vitest';
import {render, screen} from '@testing-library/react';
import {
    FormField,
    type FormFieldControlContext,
    getCompositeControlAriaProps,
    getNativeInputAriaProps,
} from './form-field';
import {FormFieldGroup} from './form-field-group';
import {
    FormFieldTokens,
    formFieldLabelRowSx,
    getFormFieldMarginSx,
} from '../../theming/form-field-tokens';

describe('FormField', () => {
    const fieldContext: FormFieldControlContext = {
        controlId: 'customer-name',
        labelId: 'customer-name-label',
        helperTextId: 'customer-name-helper-text',
        disabled: true,
        readOnly: false,
        busy: false,
        required: true,
        invalid: false,
        ariaProps: {
            'aria-labelledby': 'customer-name-label',
            'aria-describedby': 'customer-name-helper-text shared-description',
            'aria-disabled': true,
            'aria-required': true,
        },
    };

    it('merges native input ARIA without repeating the external field label', () => {
        expect(getNativeInputAriaProps(fieldContext, {
            'aria-label': 'Existing name',
            'aria-labelledby': 'autocomplete-input-label',
            'aria-describedby': 'shared-description autocomplete-help',
            'aria-disabled': false,
            'aria-readonly': true,
            'aria-required': false,
            'aria-invalid': true,
        })).toEqual({
            'aria-label': 'Existing name',
            'aria-labelledby': 'autocomplete-input-label',
            'aria-describedby': 'customer-name-helper-text shared-description autocomplete-help',
            'aria-disabled': true,
            'aria-readonly': true,
            'aria-busy': undefined,
            'aria-required': true,
            'aria-invalid': true,
        });
    });

    it('does not overwrite an existing native label association with undefined', () => {
        expect(getNativeInputAriaProps(fieldContext)).not.toHaveProperty('aria-labelledby');
    });

    it('merges composite control ARIA with the visible field label', () => {
        expect(getCompositeControlAriaProps(fieldContext, {
            'aria-label': 'Existing name',
            'aria-labelledby': 'internal-value-label customer-name-label',
            'aria-describedby': 'shared-description internal-help',
            'aria-disabled': false,
            'aria-readonly': true,
        })).toEqual({
            'aria-label': 'Existing name',
            'aria-labelledby': 'customer-name-label internal-value-label',
            'aria-describedby': 'customer-name-helper-text shared-description internal-help',
            'aria-disabled': true,
            'aria-readonly': true,
            'aria-busy': undefined,
            'aria-required': true,
            'aria-invalid': undefined,
        });
    });

    it('uses the shared control height scale and centers the label row above a four pixel gap', () => {
        expect(FormFieldTokens.controlMinHeight).toBe(44);
        expect(FormFieldTokens.controlWithSecondaryTextMinHeight).toBe(52);
        expect(FormFieldTokens.groupedControlRowMinHeight).toBe(50);
        expect(FormFieldTokens.labelToControlGap).toBe(0.5);
        expect(FormFieldTokens.helperTextGap).toBe(1);
        expect(formFieldLabelRowSx.alignItems).toBe('center');
    });

    it('keeps only a minimal leading margin in the field spacing presets', () => {
        expect(getFormFieldMarginSx('normal')).toEqual({mt: 0.25, mb: 1});
        expect(getFormFieldMarginSx('dense')).toEqual({mt: 0.125, mb: 0.5});
        expect(getFormFieldMarginSx('none')).toEqual({});
    });

    it('associates the external label and hint with the control', () => {
        render(
            <FormField
                id="customer-name"
                label="Name"
                hint="Bitte vollständig angeben."
                assistiveText="Unterstützt dynamische Inhalte."
                labelAction={(field) => (
                    <button type="button" aria-controls={field.controlId}>Optionen</button>
                )}
            >
                {(field) => (
                    <input
                        id={field.controlId}
                        {...field.ariaProps}
                    />
                )}
            </FormField>,
        );

        const input = screen.getByRole('textbox', {name: 'Name – optional'});
        expect(input).toHaveAccessibleDescription('Bitte vollständig angeben. Unterstützt dynamische Inhalte.');
        expect(input).not.toHaveAccessibleName('Name – optional Optionen');
        expect(screen.getByTitle('Name')).toHaveAttribute('for', 'customer-name');
        expect(input).toHaveAttribute('aria-labelledby', 'customer-name-label');
        expect(screen.queryByRole('group', {name: 'Name – optional'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Optionen'})).toHaveAttribute('aria-controls', 'customer-name');
    });

    it('exposes error and field states through the control contract', () => {
        render(
            <FormField
                label="Aktenzeichen"
                error="Das Aktenzeichen ist ungültig."
                required
                readOnly
                busy
            >
                {(field) => (
                    <input
                        id={field.controlId}
                        {...field.ariaProps}
                    />
                )}
            </FormField>,
        );

        const input = screen.getByRole('textbox', {name: /Aktenzeichen/});
        expect(input).toHaveAccessibleName('Aktenzeichen');
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAttribute('aria-required', 'true');
        expect(input).toHaveAttribute('aria-readonly', 'true');
        expect(input).toHaveAttribute('aria-busy', 'true');
        expect(input).toHaveAttribute('aria-disabled', 'true');
        expect(input).toHaveAccessibleDescription('Das Aktenzeichen ist ungültig.');
        expect(screen.getByRole('alert')).toHaveTextContent('Das Aktenzeichen ist ungültig.');
    });

    it('supports an explicit accessible name when no visible label is rendered', () => {
        render(
            <FormField label="" ariaLabel="Suche" margin="none">
                {(field) => (
                    <input
                        id={field.controlId}
                        {...field.ariaProps}
                    />
                )}
            </FormField>,
        );

        const input = screen.getByRole('textbox', {name: 'Suche'});
        expect(input).toBeInTheDocument();
        expect(input.id).toMatch(/^field-[A-Za-z0-9_-]+$/);
    });

    it('keeps an error state when its helper text is rendered by a parent group', () => {
        render(
            <FormField
                label="Von"
                error="Der Zeitraum ist ungültig."
                hideHelperText
            >
                {(field) => (
                    <input
                        id={field.controlId}
                        {...field.ariaProps}
                    />
                )}
            </FormField>,
        );

        const input = screen.getByRole('textbox', {name: 'Von – optional'});
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).not.toHaveAttribute('aria-describedby');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('supports nested subfields without repeating the optional indicator', () => {
        render(
            <FormField
                label="Von"
                ariaDescribedBy="range-helper"
                showOptionalIndicator={false}
            >
                {(field) => (
                    <input
                        id={field.controlId}
                        {...field.ariaProps}
                    />
                )}
            </FormField>,
        );

        const input = screen.getByRole('textbox', {name: 'Von'});
        expect(input).toHaveAttribute('aria-describedby', 'range-helper');
        expect(input).not.toHaveAccessibleName(/optional/);
    });
});

describe('FormFieldGroup', () => {
    it('uses native group semantics and labels the nested control group', () => {
        render(
            <FormFieldGroup
                id="delivery-method"
                label="Zustellung"
                hint="Wählen Sie eine Option."
                labelAction={<button type="button">Optionen</button>}
                required
            >
                {() => (
                    <div role="presentation">
                        <label><input type="radio" name="delivery"/>Digital</label>
                        <label><input type="radio" name="delivery"/>Post</label>
                    </div>
                )}
            </FormFieldGroup>,
        );

        const group = screen.getByRole('group', {name: 'Zustellung'});
        expect(group).toHaveAccessibleDescription('Wählen Sie eine Option.');
        expect(group).toHaveAttribute('aria-required', 'true');
        expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
        expect(screen.getByRole('radio', {name: 'Digital'})).toBeInTheDocument();
    });
});
