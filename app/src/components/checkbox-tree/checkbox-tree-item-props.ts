import {type CheckboxTreeOptionItem} from './checkbox-tree-option-item';

export interface CheckboxTreeItemProps {
    item: CheckboxTreeOptionItem;
    value: string[];
    onChange: (value: string[]) => void;
    disabled: boolean;
}
