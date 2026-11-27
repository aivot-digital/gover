import {isApiError} from '../../../models/api-error';

export function isTaskDeadlineExceededError(error: unknown): boolean {
    return isApiError(error) && error.status === 409 && error.details?.reason === 'task_deadline_exceeded';
}
