import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {type Preset} from '../../models/entities/preset';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type PresetVersion} from '../../models/entities/preset-version';

export interface TabProps<T extends GroupLayout, E extends PresetVersion> extends ElementEditorContentProps<T, E> {
    preset: Preset;
    onPresetChange: (preset: Preset) => void;
    storeKey: string | undefined;
}
