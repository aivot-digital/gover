import {afterEach, describe, expect, it, vi} from 'vitest';
import {
    loadListColumnSettings,
    saveListColumnSettings,
    loadListFullWidth,
    saveListFullWidth,
} from './generic-list-column-settings';
import {StorageKey} from '../../data/storage-key';
import {StorageScope, StorageService} from '../../services/storage-service';

const columns = [
    {field: 'name'},
    {field: 'extra'},
    {field: 'new'},
    {
        field: 'icon',
        hideable: false,
        resizable: false,
    },
];
const key = StorageKey.ProcessTaskListColumns;

describe('list column settings', () => {
    afterEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
    });

    it('keeps list settings separate and preserves explicit visibility when defaults differ', () => {
        saveListColumnSettings(key, columns, {
            columnVisibilityModel: {name: false},
            dimensions: {
                extra: {
                    width: 240,
                    flex: 0,
                },
            },
        });
        const restored = loadListColumnSettings(key, columns, {extra: false});
        expect(restored.columnVisibilityModel).toMatchObject({
            name: false,
            extra: true,
            icon: true,
        });
        expect(restored.dimensions).toEqual({
            extra: {
                width: 240,
                flex: 0,
            },
        });
        expect(
            loadListColumnSettings(StorageKey.ProcessInstanceListColumns, columns, {extra: false})
                .columnVisibilityModel,
        ).toEqual({extra: false});
    });

    it('uses defaults for new columns and ignores obsolete fields and invalid values', () => {
        StorageService.storeObject(
            key,
            {
                columnVisibilityModel: {
                    name: false,
                    extra: 'false',
                    obsolete: false,
                    icon: false,
                },
                dimensions: {
                    name: {width: -20},
                    extra: {width: '300'},
                    icon: {width: 600},
                },
            },
            StorageScope.Local,
        );
        const restored = loadListColumnSettings(key, columns, {
            extra: true,
            new: false,
        });
        expect(restored.columnVisibilityModel).toEqual({
            name: false,
            extra: true,
            new: false,
            icon: true,
        });
        expect(restored.dimensions).toEqual({});
    });

    it.each([null, [], 'invalid'])('falls back for malformed stored settings (%j)', (stored) => {
        vi.spyOn(StorageService, 'loadObject').mockReturnValue(stored);
        expect(loadListColumnSettings(key, columns, {extra: false}).columnVisibilityModel).toEqual({extra: false});
    });

    it('preserves full width when saving columns and keeps columns when changing display width', () => {
        saveListFullWidth(key, true);
        saveListColumnSettings(key, columns, {columnVisibilityModel: {extra: false}});
        expect(loadListFullWidth(key)).toBe(true);
        saveListFullWidth(key, false);
        expect(loadListFullWidth(key)).toBe(false);
        expect(loadListColumnSettings(key, columns).columnVisibilityModel?.extra).toBe(false);
        expect(loadListFullWidth(StorageKey.ProcessInstanceListColumns)).toBe(false);
    });

    it('keeps configuration usable when browser storage is unavailable', () => {
        vi.spyOn(StorageService, 'loadObject').mockImplementation(() => {
            throw new Error('Disabled');
        });
        vi.spyOn(StorageService, 'storeObject').mockImplementation(() => {
            throw new Error('Full');
        });
        expect(loadListColumnSettings(key, columns, {extra: false}).columnVisibilityModel).toEqual({extra: false});
        expect(() => saveListColumnSettings(key, columns, {columnVisibilityModel: {}})).not.toThrow();
        expect(loadListFullWidth(key)).toBe(false);
        expect(() => saveListFullWidth(key, true)).not.toThrow();
    });
});
