import {Ref} from 'react';

export type FunctionType = 'legacy-code' | 'legacy-condition' | 'code' | 'expression';

export interface BaseCodeTabProps {
    label: string;
    description?: string;
    requirements?: string;
    onRequirementsChange: (changedRequirement: string | undefined) => void;

    onDeleteFunction: () => void;

    hasFunction: boolean;
    functionActionsDisabled?: boolean;
    functionToolbarRef?: Ref<HTMLDivElement>;
    allowsNoCode: boolean;
    allowsExpression: boolean;
    editable: boolean;

    onSelectFunction: (type: FunctionType) => void;
}
