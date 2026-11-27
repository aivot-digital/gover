import React from 'react';
import {cloneElement} from '../../../utils/clone-element';
import {type BaseTabProps} from './base-tab-props';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {MarketplaceModuleList} from '../../../components/marketplace-module-list/marketplace-module-list';

export function MarketplaceTab(props: BaseTabProps & {
    showMarketplaceModuleId: (id: string) => void;
    highlightedModuleId?: string;
}) {
    const marketplaceKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.prosuna.marketplaceKey));

    return (
        <MarketplaceModuleList
            onSelect={(module, element) => {
                const elementToAdd = cloneElement(element, true);
                elementToAdd.marketplaceLink = {
                    marketplaceId: module.id,
                    marketplaceVersion: module.current_version,
                };
                elementToAdd.name = module.title.substring(0, 30);
                props.onAddElement(elementToAdd);
            }}
            marketplaceKey={marketplaceKey}
            itemAction={{
                icon: <InfoOutlinedIcon/>,
                tooltip: 'Mehr Informationen',
                onClick: (module) => {
                    props.showMarketplaceModuleId(module.id);
                },
            }}
            selectedModuleId={props.highlightedModuleId}
            primaryActionLabel={props.primaryActionLabel}
            primaryActionIcon={props.primaryActionIcon}
        />
    );
}
