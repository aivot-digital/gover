import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

    it('keeps technical Markdown behind the shared level-four disclosure', async () => {
        const user = userEvent.setup();
        render(<ElementDescriptionContent usage="Erfasst Angaben."
                                          technicalDetails={'# Speicherung\n\nDatentyp: **Textwert** (`String`).'}/>);

        expect(screen.getByRole('heading', {name: 'Technische Informationen', level: 4})).toBeVisible();
        expect(screen.queryByText('Textwert')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        expect(screen.getByRole('region', {name: 'Technische Informationen'})).toBeVisible();
        expect(screen.getByRole('heading', {name: 'Speicherung', level: 4})).toBeVisible();
        expect(screen.getByText('Textwert').tagName).toBe('STRONG');
        expect(screen.getByText('String').tagName).toBe('CODE');
    });
});
