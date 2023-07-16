import { type AnyElement } from '../../../../../../models/elements/any-element';
import { type EditorTab } from '../../../../../../editors';
import { type RootElement } from '../../../../../../models/elements/root-element';
import { type StepElement } from '../../../../../../models/elements/steps/step-element';
import { type GroupLayout } from '../../../../../../models/elements/form/layout/group-layout';
import { type ReplicatingContainerLayout } from '../../../../../../models/elements/form/layout/replicating-container-layout';
import { type Application } from '../../../../../../models/entities/application';
import { type Preset } from '../../../../../../models/entities/preset';

export interface ElementEditorContentProps<T extends AnyElement, E extends Application | Preset> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: T;
    entity: E;
    currentTab: string;
    additionalTabs: EditorTab[];
    onChange: (update: Partial<T>) => void;
    onChangeEntity: (update: Partial<E>) => void;
    editable: boolean;
}
