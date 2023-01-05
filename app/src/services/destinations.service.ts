import {CrudService} from './crud.service';
import {Destination} from '../models/destination';

export const DestinationsService = new CrudService<Destination, 'destinations', number>('destinations');
