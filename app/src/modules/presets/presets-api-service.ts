import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Preset} from "../../models/entities/preset";
import {CustomerInput} from '../../models/customer-input';
import {FormState} from '../../models/dtos/form-state';
import {ElementData} from '../../models/element-data';

interface PresetFilter {
    title: string;
    exactTitle: string;
    publishedInternally: boolean;
    publishedToStore: boolean;
}

export class PresetsApiService extends CrudApiService<Preset, Preset, Preset, Preset, Preset, string, PresetFilter> {
    public constructor(api: Api) {
        super(api, 'presets/');
    }

    public initialize(): Preset {
        return {
            key: '',
            title: '',
            storeId: '',
            currentVersion: '',
            currentStoreVersion: '',
            currentPublishedVersion: '',
            created: '',
            updated: '',
        };
    }

    public async determinePresetState(presetKey: string, presetVersion: string, elementData: ElementData, args: {
        disableVisibilities: boolean,
        disableValidation: boolean,
    }): Promise<ElementData> {
        return await this.api.post<ElementData>(`presets/${presetKey}/${presetVersion}/derive`, elementData, {queryParams: args});
    }
}