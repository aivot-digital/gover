import {type GroupLayout} from '../elements/form/layout/group-layout';

export interface StoreUpdateModule {
    version: string;
    changes: string;
    gover_root: GroupLayout;
}
