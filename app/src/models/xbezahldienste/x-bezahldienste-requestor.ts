import {XBezahldiensteAddress} from './x-bezahldienste-address';

export interface XBezahldiensteRequestor {
    id?: string;
    name?: string;
    email?: string;
    address?: XBezahldiensteAddress;
}