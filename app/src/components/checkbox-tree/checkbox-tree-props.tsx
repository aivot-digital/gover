import {type CheckboxTreeOption} from './checkbox-tree-option';

export interface CheckboxTreeProps {
    options: CheckboxTreeOption[];
    value: string[];
    onChange: (value: string[]) => void;
    disabled: boolean;
}
