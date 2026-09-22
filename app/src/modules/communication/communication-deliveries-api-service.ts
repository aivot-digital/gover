import {BaseApiService} from '../../services/base-api-service';

export enum CommunicationDeliveryStatus {
    Sending = 'Sending',
    Submitted = 'Submitted',
    Accepted = 'Accepted',
    Rejected = 'Rejected',
    Unknown = 'Unknown',
    Failed = 'Failed',
    Cancelled = 'Cancelled',
}

export interface CommunicationDeliveryView {
    id: string;
    status: CommunicationDeliveryStatus;
    label: string;
    message: string | null;
    receipt: {
        submissionId?: string;
        caseId?: string;
        eventId?: string;
        problems?: {type?: string; title?: string; detail?: string; instance?: string}[];
    };
    updated: string;
    overdue: boolean;
    checking: boolean;
}

export class CommunicationDeliveriesApiService extends BaseApiService {
    public async forTask(taskId: number): Promise<CommunicationDeliveryView | null> {
        const response = await this.fetch('GET', `/api/process-instance-tasks/${taskId}/delivery/`);
        return response.status === 204 ? null : response.json();
    }

    public forTest(providerId: number, deliveryId: string): Promise<CommunicationDeliveryView> {
        return this.get(`/api/communication-providers/${providerId}/tests/${deliveryId}/`);
    }
}
