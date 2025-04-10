import { CrudApiService } from '../../services/crud-api-service';
import { Api } from '../../hooks/use-api';
import { PresetVersion } from "../../models/entities/preset-version";
import {generateElementWithDefaultValues} from "../../utils/generate-element-with-default-values";
import {ElementType} from "../../data/element-type/element-type";
import type {GroupLayout} from "../../models/elements/form/layout/group-layout";

export class PresetVersionApiService extends CrudApiService<
    PresetVersion,
    PresetVersion,
    PresetVersion,
    PresetVersion,
    PresetVersion,
    string,
    {}
> {
    public constructor(api: Api, presetKey: string) {
        super(api, `presets/${presetKey}/versions`);
    }

    public initialize(): PresetVersion {
        return {
            preset: '',
            version: '',
            root: generateElementWithDefaultValues(ElementType.Container) as GroupLayout,
            publishedAt: null,
            publishedStoreAt: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
        };
    }
}
