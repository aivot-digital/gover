import {Api} from './use-api';
import {Preset} from '../models/entities/preset';
import {PresetVersion} from '../models/entities/preset-version';

interface PresetsApi {
    list(filter?: { onlyPublished?: boolean }): Promise<Preset[]>;

    retrieve(key: string): Promise<Preset>;

    save(preset: Preset): Promise<Preset>;

    destroy(key: string): Promise<void>;

    listVersions(key: string): Promise<PresetVersion[]>;

    retrieveVersion(key: string, version: string): Promise<PresetVersion>;

    createVersion(key: string, version: PresetVersion): Promise<PresetVersion>;

    updateVersion(key: string, version: PresetVersion): Promise<PresetVersion>;

    destroyVersion(key: string, version: string): Promise<void>;
}

export function usePresetsApi(api: Api): PresetsApi {
    const list = async (filter?: { onlyPublished?: boolean }) => {
        return await api.get<Preset[]>('presets', {
            queryParams: {
                published: filter?.onlyPublished,
            },
        });
    };

    const retrieve = async (key: string) => {
        return await api.get<Preset>(`presets/${key}`);
    };

    const save = async (preset: Preset) => {
        if (preset.key === '') {
            return await api.post<Preset>('presets', preset);
        } else {
            return await api.put<Preset>(`presets/${preset.key}`, preset);
        }
    };

    const destroy = async (key: string) => {
        return await api.destroy<void>(`presets/${key}`);
    };

    const listVersions = async (key: string) => {
        return await api.get<PresetVersion[]>(`preset-versions`, {queryParams: {preset: key}});
    };

    const retrieveVersion = async (key: string, version: string) => {
        return await api.get<PresetVersion>(`preset-versions/${key}/${version}`);
    };

    const createVersion = async (key: string, version: PresetVersion) => {
        return await api.post<PresetVersion>(`preset-versions`, {
            ...version,
            preset: key,
        });
    };

    const updateVersion = async (key: string, version: PresetVersion) => {
        return await api.put<PresetVersion>(`preset-versions/${key}/${version.version}`, version);
    };

    const destroyVersion = async (key: string, version: string) => {
        return await api.destroy<void>(`preset-versions/${key}/${version}`);
    };

    return {
        list,
        retrieve,
        save,
        destroy,
        listVersions,
        retrieveVersion,
        createVersion,
        updateVersion,
        destroyVersion,
    };
}