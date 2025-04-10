import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Destination} from './models/destination';
import {DestinationType} from '../../data/destination-type';

interface DestinationFilter {
    name: string;
    type: string;
}

export class DestinationsApiService extends CrudApiService<Destination, Destination, Destination, Destination, Destination, number, DestinationFilter> {
    public constructor(api: Api) {
        super(api, 'destinations/');
    }

    public initialize(): Destination {
        return {
            id: 0,
            name: '',
            type: DestinationType.Mail,
        };
    }
}