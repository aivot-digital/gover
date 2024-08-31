import {type Preset} from '../models/entities/preset';
import {type PresetVersion} from '../models/entities/preset-version';

export function determinePresetVersionDescriptor(preset: Preset, version: PresetVersion): string {
    if (version.version === preset.currentPublishedVersion) {
        return 'Veröffentlichte Version';
    }

    if (version.version === preset.currentVersion) {
        return 'Arbeits-Version';
    }

    return 'Veraltete Version';
}
