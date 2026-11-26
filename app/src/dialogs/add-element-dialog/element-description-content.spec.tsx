import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ElementDescriptionContent} from './element-description-content';

describe('ElementDescriptionContent', () => {
    it('omits optional sections without adding empty headings or technical controls', () => {
        render(<ElementDescriptionContent usage="Erfasst **Angaben**."/>);

        expect(screen.getAllByRole('heading')).toHaveLength(1);
        expect(screen.getByRole('heading', {name: 'Wofür verwenden Sie dieses Element?', level: 4})).toBeVisible();
        expect(screen.getByText('Angaben').tagName).toBe('STRONG');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
        expect(screen.queryByRole('region')).not.toBeInTheDocument();
    });
});
