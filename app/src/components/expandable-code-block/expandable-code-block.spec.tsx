import React from 'react';
import {createTheme, ThemeProvider} from '@mui/material';
import {getContrastRatio} from '@mui/material/styles';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ExpandableCodeBlock} from './expandable-code-block';
import {createCodeSyntaxTheme, type CodeBlockLanguage} from './code-syntax-theme';

describe('ExpandableCodeBlock', () => {
    it('highlights JSON without changing special characters', () => {
        const value = '{"html":"<strong>A & B</strong>"}';
        const {container} = render(
            <ExpandableCodeBlock value={value} language="json"/>,
        );

        expect(container.querySelector('code')).toHaveTextContent(value);
        expect(container.querySelector('.token.property')).toHaveTextContent('"html"');
        expect(container.querySelector('.token.string')).toHaveTextContent('"<strong>A & B</strong>"');
    });

    it.each<[CodeBlockLanguage, string]>([
        ['javascript', 'const answer = 42;'],
        ['typescript', 'const answer: number = 42;'],
    ])('highlights %s source code', (language, value) => {
        const {container} = render(
            <ExpandableCodeBlock value={value} language={language}/>,
        );

        expect(container.querySelector('.token.keyword')).toHaveTextContent('const');
        expect(container.querySelector('code')).toHaveTextContent(value);
    });

    it.each<[CodeBlockLanguage, string, string]>([
        ['html', '<section>Content</section>', '.token.tag'],
        ['css', '.panel { color: red; }', '.token.selector'],
        ['markdown', '# Heading', '.token.title'],
        ['xml', '<item id="1"/>', '.token.attr-name'],
    ])('highlights supported %s content', (language, value, tokenSelector) => {
        const {container} = render(
            <ExpandableCodeBlock value={value} language={language}/>,
        );

        expect(container.querySelector(tokenSelector)).toBeInTheDocument();
        expect(container.querySelector('code')).toHaveTextContent(value);
    });

    it.each(['light', 'dark'] as const)('keeps all custom syntax colors accessible in %s mode', (mode) => {
        const theme = createTheme({palette: {mode}});
        const syntaxTheme = createCodeSyntaxTheme(theme);

        for (const entry of syntaxTheme.styles) {
            const color = entry.style.color;
            if (color != null) {
                expect(getContrastRatio(color, theme.palette.background.default)).toBeGreaterThanOrEqual(4.5);
            }
        }
    });

    it('does not apply JSON highlighting to plain text', () => {
        const {container} = render(
            <ExpandableCodeBlock value={'Status response: {"successful": true}'}/>,
        );

        expect(container.querySelector('.token.property')).not.toBeInTheDocument();
        expect(container.querySelector('code')).toHaveTextContent('Status response: {"successful": true}');
    });

    it('renders a unified diff from complete previous and current values', () => {
        const {container} = render(
            <ExpandableCodeBlock
                language="json"
                previousValue={'{\n    "status": "old"\n}'}
                value={'{\n    "status": "new"\n}'}
            />,
        );

        const removedLine = container.querySelector('[data-diff-kind="removed"]');
        const addedLine = container.querySelector('[data-diff-kind="added"]');

        expect(removedLine).toHaveTextContent('Entfernte Zeile:');
        expect(removedLine?.textContent).toContain('-    "status": "old"');
        expect(addedLine).toHaveTextContent('Hinzugefügte Zeile:');
        expect(addedLine?.textContent).toContain('+    "status": "new"');
    });

    it('falls back to the current value when the diff input exceeds the processing limit', () => {
        const {container} = render(
            <ExpandableCodeBlock
                previousValue={'a'.repeat(130_000)}
                value={'b'.repeat(130_000)}
            />,
        );

        expect(screen.getByText('Der Vergleich wurde wegen des Datenumfangs nicht dargestellt.')).toBeVisible();
        expect(container.querySelector('[data-diff-kind="added"]')).not.toBeInTheDocument();
        expect(container.querySelector('[data-diff-kind="removed"]')).not.toBeInTheDocument();
    });

    it('uses the dark MUI background surface in dark mode', () => {
        const theme = createTheme({palette: {mode: 'dark'}});
        render(
            <ThemeProvider theme={theme}>
                <ExpandableCodeBlock value="plain text"/>
            </ThemeProvider>,
        );

        expect(screen.getByTestId('expandable-code-block')).toHaveStyle({
            backgroundColor: theme.palette.background.default,
            borderColor: theme.palette.divider,
        });
    });

    it('exposes the expanded state for long code blocks', () => {
        render(
            <ExpandableCodeBlock value={Array.from({length: 21}, (_, index) => `line ${index}`).join('\n')}/>,
        );

        const button = screen.getByRole('button', {name: 'Vollständig anzeigen'});
        expect(button).toHaveAttribute('aria-expanded', 'false');
        expect(button).toHaveClass('MuiButton-text');

        fireEvent.click(button);

        expect(screen.getByRole('button', {name: 'Weniger anzeigen'})).toHaveAttribute('aria-expanded', 'true');
    });
});
