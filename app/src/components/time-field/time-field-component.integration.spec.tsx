import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {TimeFieldComponent} from './time-field-component';

describe('TimeFieldComponent with MUI', () => {
    it('should render a local time without shifting it', () => {
        const {container} = render(
            <TimeFieldComponent
                label="Uhrzeit"
                value="09:30"
                onChange={vi.fn()}
            />,
        );

        const sections = Array
            .from(container.querySelectorAll('[contenteditable="true"]'))
            .map((section) => section.textContent);

        expect(sections).toEqual(['09', '30']);
        expect(screen.getByRole('group', {name: 'Uhrzeit – optional'})).toBeInTheDocument();
    });
});
