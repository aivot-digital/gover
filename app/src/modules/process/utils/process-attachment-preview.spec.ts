import {describe, expect, it} from 'vitest';
import {getPreviewableAttachmentMediaType} from './process-attachment-preview';

describe('getPreviewableAttachmentMediaType', () => {
    it.each([
        ['application/pdf', 'application/pdf'],
        ['image/png', 'image/png'],
        ['image/jpeg', 'image/jpeg'],
        ['image/gif', 'image/gif'],
        ['image/webp', 'image/webp'],
        ['text/plain;charset=UTF-8', 'text/plain'],
        ['Application/PDF', 'application/pdf'],
    ])('allows previewing %s', (mediaType, expected) => {
        expect(getPreviewableAttachmentMediaType(mediaType)).toBe(expected);
    });

    it.each([
        'text/html',
        'application/xhtml+xml',
        'image/svg+xml',
        'application/xml',
        'text/xml',
        'application/javascript',
        'application/octet-stream',
        '',
        null,
        undefined,
    ])('rejects previewing %s', (mediaType) => {
        expect(getPreviewableAttachmentMediaType(mediaType)).toBeNull();
    });
});
