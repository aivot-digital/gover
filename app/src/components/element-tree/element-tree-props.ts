import {type Application} from '../../models/entities/application';
import {type Preset} from '../../models/entities/preset';

export interface ElementTreeProps<T extends Application | Preset> {
    entity: T;
    onPatch: (patch: T) => void;
    editable: boolean;
}
