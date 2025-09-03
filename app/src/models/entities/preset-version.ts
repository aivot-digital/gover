import {type GroupLayout} from '../elements/form/layout/group-layout';

export interface PresetVersion {
    preset: string;
    version: string;
    rootElement: GroupLayout;
    publishedAt: string | null;
    publishedStoreAt: string | null;
    created: string;
    updated: string;
}
