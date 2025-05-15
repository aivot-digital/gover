import {type ElementTreeEntity} from './element-tree-entity';
import {type ElementTreeScope} from './element-tree-scope';
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';

export interface ElementTreeProps<T extends ElementTreeEntity> {
    entity: T;
    onPatch: (patch: T) => void;
    editable: boolean;
    scope: ElementTreeScope;
    enabledIdentityProviderInfos: IdentityProviderInfo[];
}
