import {type MarketplaceListModule} from '../../models/entities/marketplace-list-module';
import {type MarketplaceDetailModule} from '../../models/entities/marketplace-detail-module';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type ReactNode} from 'react';

export interface MarketplaceModuleListProps {
    marketplaceKey?: string;
    selectedModuleId?: string;
    onSelect: (module: MarketplaceDetailModule, element: GroupLayout) => void;
    primaryActionLabel: string;
    primaryActionIcon: ReactNode;
    itemAction?: {
        tooltip: string;
        icon: React.ReactNode;
        onClick: (module: MarketplaceListModule) => void;
    };
}
