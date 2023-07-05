import {ProviderLink} from '../models/entities/provider-link';
import {ApiService} from "./api-service";

export const ProviderLinksService = new ApiService<ProviderLink, ProviderLink, number>('provider-links');
