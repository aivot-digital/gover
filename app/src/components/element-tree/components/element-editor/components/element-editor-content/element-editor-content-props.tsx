import { type AnyElement } from '../../../../../../models/elements/any-element';
import { type EditorTab } from '../../../../../../editors';
import { type RootElement } from '../../../../../../models/elements/root-element';
import { type StepElement } from '../../../../../../models/elements/steps/step-element';
import { type GroupLayout } from '../../../../../../models/elements/form/layout/group-layout';
import { type ReplicatingContainerLayout } from '../../../../../../models/elements/form/layout/replicating-container-layout';
import { type Application } from '../../../../../../models/entities/application';

export interface ElementEditorContentProps<T extends AnyElement> {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: T;
    application: Application;
    currentTab: string;
    additionalTabs: EditorTab[];
    onChange: (update: Partial<T>) => void;
    onChangeApplication: (update: Partial<Application>) => void;
    editable: boolean;
}
