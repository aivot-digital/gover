import {GroupLayout} from "../elements/form/layout/group-layout";

export interface StoreCreateModule {
    version: string;
    title: string;
    description: string;
    description_short: string;
    is_public: boolean;
    datenfeld_id: string;
    gover_root: GroupLayout;
}