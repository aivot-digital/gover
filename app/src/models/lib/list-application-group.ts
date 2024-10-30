import {FormListProjection} from '../entities/form';

export interface ListApplicationGroup {
    slug: string;
    applications: FormListProjection[];
}