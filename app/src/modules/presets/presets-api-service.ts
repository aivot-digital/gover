import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {PresetCreateReqeustDTO, Preset} from "../../models/entities/preset";
import {ElementData, ElementDerivationResponse} from '../../models/element-data';

interface PresetFilter {
    title: string;
    published: boolean;
}

export class PresetsApiService extends CrudApiService<PresetCreateReqeustDTO, Preset, Preset, Preset, Preset, string, PresetFilter> {
    public constructor(api: Api) {
        super(api, 'presets/');
    }

    public initialize(): Preset {
        return {
            key: '',
            title: '',
            draftedVersion: null,
            publishedVersion: null,
            created: '',
            updated: '',
        };
    }

    public async determinePresetState(presetKey: string, presetVersion: number, elementData: ElementData, args: {
        disableVisibilities: boolean,
        disableValidation: boolean,
    }): Promise<ElementDerivationResponse> {
        return await this.api.post<ElementDerivationResponse>(`presets/${presetKey}/${presetVersion}/derive`, elementData, {queryParams: args});
    }
}