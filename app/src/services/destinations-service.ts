import { type Destination } from '../models/entities/destination';
import { ApiService } from './api-service';
import { type ListApplication } from '../models/entities/list-application';


class _DestinationsService extends ApiService<Destination, Destination, number> {
    constructor() {
        super('destinations');
    }

    public async listApplications(destinationId: number): Promise<ListApplication[]> {
        return await ApiService.get<ListApplication[]>(`destinations/${ destinationId }/applications`);
    }
}

export const DestinationsService = new _DestinationsService();
