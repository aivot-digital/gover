import {type GroupLayout} from '../elements/form/layout/group-layout';
import {FormStatus} from '../../modules/forms/enums/form-status';

export interface PresetVersion {
    presetKey: string;
    version: number;
    rootElement: GroupLayout;
    status: FormStatus;
    published: string | null | undefined;
    revoked: string | null | undefined;
    created: string;
    updated: string;
}
