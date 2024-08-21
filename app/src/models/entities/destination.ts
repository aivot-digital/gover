import {DestinationType} from "../../data/destination-type";


export interface Destination {
    id: number;
    name: string;
    type: DestinationType;
    created: string;
    updated: string;

    mailTo?: string;
    mailCC?: string;
    mailBCC?: string;

    apiAddress?: string;
    authorizationHeader?: string;

    maxAttachmentMegaBytes?: number;
}
