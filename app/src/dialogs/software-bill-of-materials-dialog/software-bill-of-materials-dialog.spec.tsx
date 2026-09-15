import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach, describe, expect, it, vi} from 'vitest';
import React from 'react';
import {SoftwareBillOfMaterialsDialog} from './software-bill-of-materials-dialog';

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

describe('SoftwareBillOfMaterialsDialog', () => {
    it('shows build metadata and all download options', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(Response.json(manifest)));
        const user = userEvent.setup();

        render(<SoftwareBillOfMaterialsDialog open onClose={vi.fn()}/>);

        expect(await screen.findByText('1.408')).toBeInTheDocument();
        expect(screen.getByText('CycloneDX 1.7')).toBeInTheDocument();
        expect(screen.getByText(/ Uhr$/)).toBeInTheDocument();
        expect(screen.getByText('SBOM (JSON)').closest('a'))
            .toHaveAttribute('href', '/sbom/sbom.json');
        expect(screen.getByText('Lizenzliste (CSV)').closest('a'))
            .toHaveAttribute('href', '/sbom/licenses.csv');

        await user.click(screen.getByText('Weitere'));

        expect(screen.getByText('Frontend-SBOM (.json)').closest('a'))
            .toHaveAttribute('href', '/sbom/partials/sbom.frontend.json');
        expect(screen.getByText('Lizenzliste (.txt)').closest('a'))
            .toHaveAttribute('href', '/sbom/licenses.txt');
    });

    it('shows a neutral state when a development build has no bundle', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, {status: 404})));

        render(<SoftwareBillOfMaterialsDialog open onClose={vi.fn()}/>);

        expect(await screen.findByText(
            'Für diesen Entwicklungsbuild wurde keine Software Bill of Materials erzeugt.',
        )).toBeInTheDocument();
    });

    it('ignores a manifest response after its request was aborted', async () => {
        let resolveFirstRequest: (response: Response) => void = () => undefined;
        const firstRequest = new Promise<Response>((resolve) => {
            resolveFirstRequest = resolve;
        });
        const currentManifest = {
            ...manifest,
            version: '6.0.0',
        };
        vi.stubGlobal('fetch', vi.fn()
            .mockReturnValueOnce(firstRequest)
            .mockResolvedValueOnce(Response.json(currentManifest)));

        const {rerender} = render(<SoftwareBillOfMaterialsDialog open onClose={vi.fn()}/>);
        rerender(<SoftwareBillOfMaterialsDialog open={false} onClose={vi.fn()}/>);
        rerender(<SoftwareBillOfMaterialsDialog open onClose={vi.fn()}/>);

        expect(await screen.findByText('6.0.0')).toBeInTheDocument();

        await act(async () => {
            resolveFirstRequest(Response.json(manifest));
        });

        expect(screen.getByText('6.0.0')).toBeInTheDocument();
        expect(screen.queryByText('5.0.0')).not.toBeInTheDocument();
    });
});
