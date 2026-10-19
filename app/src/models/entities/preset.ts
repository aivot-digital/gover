import {GroupLayout} from '../elements/form/layout/group-layout';

export interface Preset {
    key: string;
    title: string;
    publishedVersion: number | null | undefined;
    draftedVersion: number | null | undefined;
    created: string;
    updated: string;
}

export interface PresetCreateReqeustDTO {
    title: string;
    rootElement: GroupLayout;
}