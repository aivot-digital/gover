import fs from 'fs';
import path from 'path';
import {
    applyHtmlTemplateSlotValues,
    parseHtmlTemplateSlots,
} from './html-template-input-utils';

function loadAlbatrosTemplate(): string {
    return fs.readFileSync(
        path.resolve(__dirname, '../../../../default-assets/Vorlagen/Briefe/Standardbrief - Albatros.html'),
        'utf-8',
    );
}

describe('html-template-input-utils', () => {
    it('parses supported slots from the Albatros template', () => {
        const parsed = parseHtmlTemplateSlots(loadAlbatrosTemplate());

        expect(parsed.unsupportedSlots).toEqual([]);
        expect(parsed.slots).toEqual(expect.arrayContaining([
            expect.objectContaining({
                id: 'header_left',
                type: 'text',
            }),
            expect.objectContaining({
                id: 'logo',
                type: 'image',
            }),
            expect.objectContaining({
                id: 'absender',
                type: 'text',
            }),
            expect.objectContaining({
                id: 'anschrift',
                type: 'richtext',
            }),
        ]));
    });

    it('applies text, image and richtext slot values to the preview document', () => {
        const previewHtml = applyHtmlTemplateSlotValues(
            loadAlbatrosTemplate(),
            {
                header_left: 'Neuer Header',
                logo: 'logoAssetKey',
                anschrift: '**Max Mustermann**',
            },
            (markdown) => `<strong>${markdown}</strong>`,
        );

        expect(previewHtml).toContain('Neuer Header');
        expect(previewHtml).toContain('/api/public/assets/logoAssetKey/');
        expect(previewHtml).toContain('<strong>**Max Mustermann**</strong>');
    });
});
