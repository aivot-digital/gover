import React from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {MetaElement} from './meta-element';

describe('MetaElement', () => {
    let originalFaviconUrl: string | null;

    beforeEach(() => {
        originalFaviconUrl = AppConfig.faviconUrl;
        AppConfig.faviconUrl = '/system-favicon.ico';
    });

    afterEach(() => {
        AppConfig.faviconUrl = originalFaviconUrl;
    });

    it('uses the Prosuna favicon for an explicitly media-less theme', () => {
        const html = renderToStaticMarkup(<MetaElement faviconUrl={null}/>);

        expect(html).toContain('rel="icon" href="/favicon.svg"');
        expect(html).not.toContain('apple-touch-icon');
        expect(html).not.toContain('/system-favicon.ico');
    });

    it('uses the system theme favicon when no specific theme is supplied', () => {
        const html = renderToStaticMarkup(<MetaElement/>);

        expect(html).toContain('rel="icon" href="/system-favicon.ico"');
    });

    it('uses the Prosuna favicon when the system theme has no favicon', () => {
        AppConfig.faviconUrl = null;

        const html = renderToStaticMarkup(<MetaElement/>);

        expect(html).toContain('rel="icon" href="/favicon.svg"');
    });

    it('uses the supplied theme favicon', () => {
        const html = renderToStaticMarkup(<MetaElement faviconUrl="/theme-favicon.ico"/>);

        expect(html).toContain('rel="icon" href="/theme-favicon.ico"');
    });
});
