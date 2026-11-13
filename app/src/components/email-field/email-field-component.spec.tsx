import {describe, expect, it, vi} from 'vitest';
import {useState} from 'react';
import {render, screen, fireEvent} from '@testing-library/react';
import {EmailFieldComponent} from './email-field-component';

describe('EmailFieldComponent', () => {
    it.each([false, true])('preserves input attributes supplied through slot props (callback=%s)', callback => {
        const htmlInput = {maxLength: 254, 'aria-label': 'Kontaktadresse'};
        render(<EmailFieldComponent label="" value={null} onChange={vi.fn()} muiPassTroughProps={{
            slotProps: {htmlInput: callback ? () => htmlInput : htmlInput},
        }} />);
        const input = screen.getByRole('textbox', {name: 'Kontaktadresse'});
        expect(input).toHaveAttribute('maxlength', '254');
        expect(input).toHaveAttribute('inputmode', 'email');
    });
    it('uses accessible labels and the email keyboard without native IDN restrictions', async () => {
        const onBlur = vi.fn();
        function Field() {
            const [value, setValue] = useState<string | null>(' Person@büro.de ');
            return <EmailFieldComponent label="E-Mail-Adresse" hint="Für Rückfragen" value={value} onChange={setValue} onBlur={onBlur} />;
        }
        render(<Field />);
        const input = screen.getByRole('textbox', {name: 'E-Mail-Adresse – optional'});
        expect(input).toHaveAccessibleDescription('Für Rückfragen');
        expect(input).toHaveAttribute('inputmode', 'email');
        expect(input).toHaveAttribute('autocomplete', 'email');
        fireEvent.blur(input);
        expect(onBlur).toHaveBeenLastCalledWith('Person@xn--bro-hoa.de');
        expect(input).toHaveValue('Person@xn--bro-hoa.de');
    });
    it('validates local addresses according to the option after blur', async () => {
        const props = {label: 'E-Mail', value: 'name@verwaltung', onChange: vi.fn()};
        const {rerender} = render(<EmailFieldComponent {...props} />);
        fireEvent.blur(screen.getByRole('textbox'));
        expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
        rerender(<EmailFieldComponent {...props} allowLocalDomains />);
        expect(screen.getByRole('textbox')).not.toHaveAttribute('aria-invalid', 'true');
    });
    it.each(['disabled', 'readonly', 'busy'] as const)('does not emit edits while %s', async state => {
        const onChange = vi.fn();
        render(<EmailFieldComponent label="E-Mail" value=" Person@EXAMPLE.org " onChange={onChange} {...{[state]: true}} />);
        fireEvent.blur(screen.getByRole('textbox'));
        expect(onChange).not.toHaveBeenCalled();
    });
});
