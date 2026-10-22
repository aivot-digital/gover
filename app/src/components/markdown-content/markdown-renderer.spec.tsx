import fs from 'fs';
import path from 'path';
import {render, screen} from '@testing-library/react';
import {renderToStaticMarkup} from 'react-dom/server';
import {describe, expect, it} from 'vitest';
import {MarkdownRenderer, sanitizeMarkdownUrl} from './markdown-renderer';

interface MarkdownRenderingCase {
    name: string;
    markdown: string;
    contains: string[];
    excludes: string[];
}

// The backend MarkdownRendererTest runs the same cases, which keeps both renderers aligned.
function loadSharedRenderingCases(): MarkdownRenderingCase[] {
    const file = path.resolve(__dirname, '../../../../backend/src/test/resources/markdown/markdown-rendering-cases.json');
    return JSON.parse(fs.readFileSync(file, 'utf-8')).cases;
}

describe('MarkdownRenderer', () => {
    it.each(loadSharedRenderingCases())('$name', (renderingCase) => {
        const html = renderToStaticMarkup(<MarkdownRenderer markdown={renderingCase.markdown}/>);

        for (const expected of renderingCase.contains) {
            expect(html).toContain(expected);
        }
        for (const unexpected of renderingCase.excludes) {
            expect(html).not.toContain(unexpected);
        }
    });

    it('keeps reducing images to their alternative text when callers override components', () => {
        const html = renderToStaticMarkup(
            <MarkdownRenderer
                markdown="![Logo](https://example.org/logo.png)"
                components={{img: (props) => <img {...props}/>}}
            />,
        );

        expect(html).toBe('<p>Logo</p>');
    });

    it('names the footnotes for screen readers only', () => {
        render(<MarkdownRenderer markdown={'Referenz[^1]\n\n[^1]: Fußnote'}/>);

        expect(screen.getByRole('heading', {name: 'Fußnoten'})).toHaveClass('visually-hidden');
        expect(screen.getByRole('link', {name: 'Zurück zum Verweis auf Fußnote 1'})).toBeInTheDocument();
    });
});

describe('sanitizeMarkdownUrl', () => {
    it.each([
        ['https://example.org', 'https://example.org'],
        [' \nmailto:info@example.org\t', 'mailto:info@example.org'],
        ['/relative/path:with-colon', '/relative/path:with-colon'],
        ['#anchor', '#anchor'],
        ['JavaScript:alert(1)', ''],
        ['data:text/html,x', ''],
        ['irc://example.org', ''],
    ])('sanitizes %j to %j', (url, expected) => {
        expect(sanitizeMarkdownUrl(url)).toBe(expected);
    });
});
