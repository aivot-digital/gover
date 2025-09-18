import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {PresetVersion} from '../../models/entities/preset-version';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import type {GroupLayout} from '../../models/elements/form/layout/group-layout';
import {FormStatus} from '../forms/enums/form-status';

export class PresetVersionApiService extends CrudApiService<
    PresetVersion,
    PresetVersion,
    PresetVersion,
    PresetVersion,
    PresetVersion,
    number,
    {}
> {
    public constructor(api: Api, presetKey: string) {
        super(api, `presets/${presetKey}/versions`);
    }

    public initialize(): PresetVersion {
        return {
            presetKey: '',
            version: 1,
            status: FormStatus.Drafted,
            rootElement: generateElementWithDefaultValues(ElementType.Container) as GroupLayout,
            published: null,
            revoked: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }

    public async publish(presetKey: string, version: number): Promise<PresetVersion> {
        return await this.api.put<PresetVersion>(`presets/${presetKey}/versions/${version}/publish/`, {});
    }

    public async revoke(presetKey: string, version: number): Promise<PresetVersion> {
        return await this.api.put<PresetVersion>(`presets/${presetKey}/versions/${version}/revoke/`, {});
    }
}
