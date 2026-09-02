import {describe, expect, it} from 'vitest';
import {getFileTypeIcon, getFileTypeIconForFile} from './file-type-icon';

describe('file type icons', () => {
    it('resolves stored files by their file extension', () => {
        expect(getFileTypeIconForFile('antrag.pdf').type)
            .toBe(getFileTypeIcon('application/pdf').type);
        expect(getFileTypeIconForFile('foto.JPG').type)
            .toBe(getFileTypeIcon('image/jpeg').type);
    });

    it('prefers a specific content type and normalizes MIME parameters', () => {
        expect(getFileTypeIconForFile('daten.bin', 'application/json; charset=utf-8').type)
            .toBe(getFileTypeIcon('application/json').type);
    });

    it('uses the file extension when only a generic content type is available', () => {
        expect(getFileTypeIconForFile('tabelle.csv', 'application/octet-stream').type)
            .toBe(getFileTypeIcon('text/csv').type);
    });
});
