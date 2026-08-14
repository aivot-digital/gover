import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ChipInputFieldComponent} from './chip-input-field-component';

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

describe('ChipInputFieldComponent', () => {
    it('preserves the accessible label supplied by Autocomplete', () => {
        render(
            <ChipInputFieldComponent
                label="Tags"
                value={null}
                onChange={vi.fn()}
            />,
        );

        expect(screen.getByRole('combobox', {name: 'Tags'})).toBeInTheDocument();
    });
});
