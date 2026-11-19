import {describe, expect, it, vi} from 'vitest';
import {AssetsApiService} from './assets-api-service';
import {type Asset} from './models/asset';

const UPDATED = '2030-05-01T12:00:00.000123Z';

const viewItem = {
    assetKey: 'key-1',
    storageProviderId: 42,
    pathFromRoot: '/images/existing.pdf',
    filename: 'existing.pdf',
    created: '2030-05-01T10:00:00Z',
    updated: UPDATED,
    assetIsPrivate: false,
};

const asset: Asset = {
    key: 'key-1',
    storageProviderId: 42,
    storagePathFromRoot: '/images/existing.pdf',
    filename: 'existing.pdf',
    created: '2030-05-01T10:00:00Z',
    updated: UPDATED,
    uploaderId: '',
    isPrivate: false,
};

describe('AssetsApiService', () => {
    it('keeps the timestamp of a loaded file', async () => {
        const service = new AssetsApiService();
        vi.spyOn(service, 'get').mockResolvedValue(viewItem as never);

        expect((await service.retrieveInStorageProvider('/images/existing.pdf', 42)).updated).toBe(UPDATED);
    });

    it('states the loaded timestamp when replacing the content', async () => {
        const service = new AssetsApiService();
        const putFormData = vi.spyOn(service, 'putFormData').mockResolvedValue(viewItem as never);

        await service.updateInStorageProvider('/images/existing.pdf', asset, 42, new File(['x'], 'existing.pdf'));

        expect(putFormData.mock.calls[0][2]).toEqual({query: {basedOnUpdated: UPDATED}});
    });

    it('does not state a timestamp for a privacy change alone', async () => {
        const service = new AssetsApiService();
        const putFormData = vi.spyOn(service, 'putFormData').mockResolvedValue(viewItem as never);

        await service.updateInStorageProvider('/images/existing.pdf', asset, 42);

        expect(putFormData.mock.calls[0][2]).toEqual({query: {basedOnUpdated: undefined}});
    });

    it('states the loaded timestamp when saving metadata', async () => {
        const service = new AssetsApiService();
        const put = vi.spyOn(service, 'put').mockResolvedValue(viewItem as never);

        await service.updateMetadataInStorageProvider('/images/existing.pdf', {color: 'blue'}, 42, UPDATED);

        expect(put.mock.calls[0][1]).toEqual({color: 'blue'});
        expect(put.mock.calls[0][2]).toEqual({query: {basedOnUpdated: UPDATED}});
    });
});
