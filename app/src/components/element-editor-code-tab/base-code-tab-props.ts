export type FunctionType = 'legacy-code' | 'legacy-condition' | 'code' | 'expression';

export interface BaseCodeTabProps {
    label: string;
    requirements?: string;
    onRequirementsChange: (changedRequirement: string | undefined) => void;

    onDeleteFunction: () => void;

    hasFunction: boolean;
    allowsNoCode: boolean;
    allowsExpression: boolean;
    editable: boolean;

    onSelectFunction: (type: FunctionType) => void;
}