import {CrudService} from './crud.service';
import {ProviderLink} from '../models/entities/provider-link';

export const ProviderLinksService = new CrudService<ProviderLink, 'providerLinks', number>('provider-links');
