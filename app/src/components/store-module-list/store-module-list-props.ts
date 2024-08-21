import {type StoreListModule} from '../../models/entities/store-list-module';
import {type StoreDetailModule} from '../../models/entities/store-detail-module';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';

export interface StoreModuleListProps {
    storeKey?: string;
    selectedModuleId?: string;
    onSelect: (module: StoreDetailModule, element: GroupLayout) => void;
    itemAction?: {
        tooltip: string;
        icon: JSX.Element;
        onClick: (module: StoreListModule) => void;
    };
}
