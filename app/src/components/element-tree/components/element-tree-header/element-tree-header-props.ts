import { type Application } from '../../../../models/entities/application';
import { type Preset } from '../../../../models/entities/preset';
import { type RootElement } from '../../../../models/elements/root-element';
import { type GroupLayout } from '../../../../models/elements/form/layout/group-layout';

export interface ElementTreeHeaderProps<T extends RootElement | GroupLayout, E extends Application | Preset> {
    entity: E;
    element: T;
    onPatch: (updatedElement: Partial<T>, updatedEntity: Partial<E>) => void;
    onToggleSearch: () => void;
    editable: boolean;
}
