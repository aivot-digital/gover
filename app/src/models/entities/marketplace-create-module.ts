import {GroupLayout} from '../elements/form/layout/group-layout';

export interface MarketplaceCreateModule {
    version: string;
    title: string;
    description: string;
    description_short: string;
    is_public: boolean;
    datenfeld_id: string;
    prosuna_root: GroupLayout;
}