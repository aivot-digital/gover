import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {type Preset} from '../../../models/entities/preset';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {PresetVersion} from '../../../models/entities/preset-version';

export interface AddPresetDialogProps extends DialogProps {
    onClose: () => void;
    onAdded: (preset: Preset, version: PresetVersion) => void;
    root?: GroupLayout;
}
