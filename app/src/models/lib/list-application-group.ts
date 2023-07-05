import {ListApplication} from "../entities/list-application";

export interface ListApplicationGroup {
    slug: string;
    applications: ListApplication[];
}