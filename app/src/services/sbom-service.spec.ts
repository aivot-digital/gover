import {afterEach, describe, expect, it, vi} from 'vitest';
import {
    createSbomFileUrl,
    loadSbomManifest,
    parseSbomManifest,
    SbomUnavailableError,
} from './sbom-service';

const manifest = {
    schemaVersion: 1,
    name: 'prosuna',
    version: '5.0.0',
    commit: '81fb0aaf0006881aa444d55c119ce938cf401bef',
    buildDate: '2026-08-07T12:09:42Z',
    generatedAt: '2026-08-07T12:11:08Z',
    sbomFormat: 'CycloneDX',
    sbomSpecVersion: '1.7',
    componentCount: 1408,
    sboms: {
        frontend: 'partials/sbom.frontend.json',
        mails: 'partials/sbom.mails.json',
        backend: 'partials/sbom.backend.json',
        merged: 'sbom.json',
    },
    reports: [
        'licenses.csv',
        'licenses.txt',
    ],
};

afterEach(() => {
    vi.unstubAllGlobals();
});

describe('SBOM service', () => {
    it('parses a manifest and resolves its files below the public SBOM path', () => {
        const parsed = parseSbomManifest(manifest);

        expect(parsed.componentCount).toBe(1408);
        expect(createSbomFileUrl(parsed.sboms.merged)).toBe('/sbom/sbom.json');
        expect(createSbomFileUrl(parsed.sboms.frontend)).toBe('/sbom/partials/sbom.frontend.json');
    });

    it('keeps manifests from existing builds compatible', () => {
        const parsed = parseSbomManifest({
            ...manifest,
            schemaVersion: undefined,
            generatedAt: undefined,
            componentCount: undefined,
        });

        expect(parsed.schemaVersion).toBeUndefined();
        expect(parsed.componentCount).toBeUndefined();
    });

    it.each([
        '',
        ' sbom.json',
        '../outside.json',
        '/outside.json',
        'https://example.com/sbom.json',
        'sbom.json?redirect=https://example.com',
    ])('rejects unsafe bundle path %s', (path) => {
        expect(() => parseSbomManifest({
            ...manifest,
            sboms: {
                ...manifest.sboms,
                merged: path,
            },
        })).toThrow(/Invalid SBOM/);
    });

    it('loads and validates the public manifest without using a browser cache', async () => {
        const fetchMock = vi.fn().mockResolvedValue(Response.json(manifest));
        vi.stubGlobal('fetch', fetchMock);

        await expect(loadSbomManifest()).resolves.toMatchObject({
            name: 'prosuna',
            componentCount: 1408,
        });
        expect(fetchMock).toHaveBeenCalledWith('/sbom/manifest.json', expect.objectContaining({
            cache: 'no-store',
        }));
    });

    it('treats a missing manifest as an unavailable SBOM bundle', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, {status: 404})));

        await expect(loadSbomManifest()).rejects.toBeInstanceOf(SbomUnavailableError);
    });

    it('recognizes an HTML response from the SPA fallback as unavailable', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<!doctype html>', {
            status: 200,
            headers: {
                'Content-Type': 'text/html; charset=utf-8',
            },
        })));

        await expect(loadSbomManifest()).rejects.toBeInstanceOf(SbomUnavailableError);
    });

    it('keeps server failures distinct from an unavailable development bundle', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Internal Server Error', {
            status: 500,
            headers: {
                'Content-Type': 'text/html; charset=utf-8',
            },
        })));

        await expect(loadSbomManifest()).rejects.toThrow('Could not load SBOM manifest (500).');
    });
});
