import {ApplicationStatus} from "../../data/application-status/application-status";
import {RootElement} from "../elements/root-element";


export interface Application {
    id: number;
    slug: string;
    version: string;
    status: ApplicationStatus;
    root: RootElement;
    created: string;
    updated: string;
}
