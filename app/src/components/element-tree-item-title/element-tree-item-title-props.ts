import {type AnyElement} from '../../models/elements/any-element';
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';

export interface ElementTreeItemTitleProps<T extends AnyElement> {
    element: T;
    isExpanded?: boolean;
    onToggleExpanded?: () => void;
    onShowAddDialog?: () => void;
    onSelect: () => void;
    editable: boolean;
    enabledIdentityProviderInfos: IdentityProviderInfo[];
}
