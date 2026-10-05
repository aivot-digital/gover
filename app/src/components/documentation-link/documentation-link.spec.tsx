import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {DocumentationLink} from './documentation-link';

describe('DocumentationLink', () => {
    it('renders a safe external link for a configured URL', () => {
        render(<DocumentationLink url="https://docs.example.com/example"/>);

        const link = screen.getByRole('link', {name: 'Dokumentation öffnen'});
        expect(link).toHaveAttribute('href', 'https://docs.example.com/example');
        expect(link).toHaveAttribute('target', '_blank');
        expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('renders nothing for a missing or blank URL', () => {
        const {rerender} = render(<DocumentationLink url={null}/>);

        expect(screen.queryByRole('link', {name: 'Dokumentation öffnen'})).not.toBeInTheDocument();

        rerender(<DocumentationLink url="   "/>);

        expect(screen.queryByRole('link', {name: 'Dokumentation öffnen'})).not.toBeInTheDocument();
    });
});
