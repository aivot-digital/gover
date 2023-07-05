import {Destination} from '../models/entities/destination';
import {ApiService} from "./api-service";

export const DestinationsService = new ApiService<Destination, Destination, number>('destinations');
