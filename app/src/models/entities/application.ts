import {RootElement} from './elements/root-element';
import {ApplicationStatus} from "../data/application-status/application-status";

export interface Application {
    id: number;
    slug: string;
    version: string;
    status: ApplicationStatus;
    root: RootElement;
    created: string;
    updated: string;
}
