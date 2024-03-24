import {type Preset} from '../../../models/entities/preset';

export interface VersionsPresetDialogProps {
    preset: Preset;
    onClose: () => void;
}
