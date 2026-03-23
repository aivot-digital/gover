import {SubmissionStatusResponseDTO} from './dtos/submission-status-response-dto';
import {BaseApiService} from '../../services/base-api-service';


export class SubmissionsApiService extends BaseApiService {

    public async getStatus(id: string): Promise<SubmissionStatusResponseDTO> {
        return await this.get<SubmissionStatusResponseDTO>(`/api/public/status/${id}/`, {
            skipAuthCheck: true,
        });
    }
}