import {type StoreListModule} from '../../models/entities/store-list-module';
import {type StoreDetailModule} from '../../models/entities/store-detail-module';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ReactNode} from 'react';

export interface StoreModuleListProps {
    storeKey?: string;
    selectedModuleId?: string;
    onSelect: (module: StoreDetailModule, element: GroupLayout) => void;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    itemAction?: {
        tooltip: string;
        icon: React.ReactNode;
        onClick: (module: StoreListModule) => void;
    };
}
