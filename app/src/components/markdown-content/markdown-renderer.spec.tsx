import fs from 'fs';
import path from 'path';
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

    it('writes link targets next to their text when revealing link targets', () => {
        const html = renderToStaticMarkup(
            <MarkdownRenderer
                markdown={'Bitte bestätigen Sie hier: [Zum Antrag](https://example.org/gefaelscht)\n[Datei](file:///etc/passwd)\n[Referenz][1]\n\n[1]: https://example.org/referenz'}
                revealLinkTargets
            />,
        );

        expect(html).toContain('Bitte bestätigen Sie hier: Zum Antrag (<a href="https://example.org/gefaelscht" target="_blank" rel="noopener noreferrer">https://example.org/gefaelscht</a>)');
        expect(html).toContain('Datei (file:///etc/passwd)');
        expect(html).toContain('Referenz (<a href="https://example.org/referenz" target="_blank" rel="noopener noreferrer">https://example.org/referenz</a>)');
        expect(html).not.toContain('>Zum Antrag<');
    });

    it('keeps revealed targets with every allowed protocol clickable', () => {
        const html = renderToStaticMarkup(
            <MarkdownRenderer
                markdown="[Mail](mailto:info@example.org) [Telefon](tel:+4912345) [SMS](sms:+4912345) [FTP](ftp://example.org/a)"
                revealLinkTargets
            />,
        );

        expect(html).toBe('<p>Mail (<a href="mailto:info@example.org">mailto:info@example.org</a>) '
            + 'Telefon (<a href="tel:+4912345">tel:+4912345</a>) '
            + 'SMS (<a href="sms:+4912345">sms:+4912345</a>) '
            + 'FTP (ftp://example.org/a)</p>');
    });

    it('keeps links that show their target when revealing link targets', () => {
        const html = renderToStaticMarkup(
            <MarkdownRenderer
                markdown={'https://example.org/a <https://example.org/b> [https://example.org/c](https://example.org/c) [www.example.org](http://www.example.org) info@example.org'}
                revealLinkTargets
            />,
        );

        expect(html).toContain('href="https://example.org/a"');
        expect(html).toContain('href="https://example.org/b"');
        expect(html).toContain('href="https://example.org/c"');
        expect(html).toContain('href="http://www.example.org"');
        expect(html).toContain('href="mailto:info@example.org"');
        expect(html).not.toContain(' (');
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
