import {DestinationType} from '../data/destination-type/destination-type';

export interface Destination {
    id: number;
    name: string;
    type: DestinationType;

    mailTo?: string;
    mailCC?: string;
    mailBCC?: string;

    apiAddress?: string;
    authorizationHeader?: string;

    // TODO: This is MB not bytes!!!
    maxAttachmentBytes?: number;
}
