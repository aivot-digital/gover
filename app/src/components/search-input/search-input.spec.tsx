import {act, fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {SearchInput} from './search-input';

describe('SearchInput', () => {
    it('uses an external label and compact search control without an optional suffix', () => {
        render(
            <SearchInput
                label="Vorgänge durchsuchen"
                value="Berlin"
                onChange={vi.fn()}
                placeholder="Aktenzeichen oder Bezeichnung eingeben"
            />,
        );

        const label = screen.getByTitle('Vorgänge durchsuchen');
        const input = screen.getByRole('searchbox', {name: 'Vorgänge durchsuchen'});

        expect(label).toHaveAttribute('for', input.id);
        expect(input).toHaveValue('Berlin');
        expect(input).not.toHaveAccessibleName(/optional/);
        expect(input.closest('[data-form-field]')).not.toHaveTextContent('optional');
        expect(document.querySelector('.MuiInputLabel-root')).not.toBeInTheDocument();
        expect(getComputedStyle(input.closest('.MuiInputBase-root')!).minHeight).toBe('44px');
    });

    it('clears immediately and keeps the clear action out of the field name', async () => {
        const user = userEvent.setup();
        const onChange = vi.fn();

        render(
            <SearchInput
                label="Nutzer suchen"
                value="Mustermann"
                onChange={onChange}
            />,
        );

        const input = screen.getByRole('searchbox', {name: 'Nutzer suchen'});
        const clearButton = screen.getByRole('button', {name: 'Suche löschen'});
        expect(input).not.toHaveAccessibleName(/löschen/);

        await user.click(clearButton);

        expect(input).toHaveValue('');
        expect(onChange).toHaveBeenCalledWith('');
    });

    it('supports a visually hidden label for compact placements', () => {
        render(
            <SearchInput
                label="Element suchen"
                ariaLabel="Elemente filtern"
                value=""
                onChange={vi.fn()}
                hideLabel
            />,
        );

        expect(screen.getByRole('searchbox', {name: 'Elemente filtern'})).toBeInTheDocument();
        expect(screen.queryByTitle('Element suchen')).not.toBeInTheDocument();
    });

    it('commits a trimmed pending debounced value on blur', () => {
        const onChange = vi.fn();

        render(
            <SearchInput
                label="Akten suchen"
                value=""
                onChange={onChange}
                debounce={500}
            />,
        );

        const input = screen.getByRole('searchbox', {name: 'Akten suchen'});
        fireEvent.change(input, {target: {value: '  AZ-42  '}});
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.blur(input);
        expect(onChange).toHaveBeenCalledWith('AZ-42');
        expect(input).toHaveValue('AZ-42');
    });

    it('does not recommit a debounced value on a later blur', () => {
        vi.useFakeTimers();
        try {
            const onChange = vi.fn();
            render(
                <SearchInput
                    label="Akten suchen"
                    value=""
                    onChange={onChange}
                    debounce={500}
                />,
            );

            const input = screen.getByRole('searchbox', {name: 'Akten suchen'});
            fireEvent.change(input, {target: {value: 'AZ-42'}});
            act(() => vi.advanceTimersByTime(500));
            expect(onChange).toHaveBeenCalledTimes(1);

            fireEvent.blur(input);
            expect(onChange).toHaveBeenCalledTimes(1);
        } finally {
            vi.useRealTimers();
        }
    });
});
