import {type Preset} from '../../../models/entities/preset';
import {type PresetVersion} from '../../../models/entities/preset-version';
import {type StoreDetailModule} from '../../../models/entities/store-detail-module';

export interface PublishPresetVersionDialogProps {
    preset: Preset;
    presetVersion: PresetVersion;
    onClose: () => void;
    onPublish: (res: StoreDetailModule) => void;
}
