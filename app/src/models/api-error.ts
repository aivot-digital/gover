export class ApiError extends Error {
    constructor(public readonly status: number, public readonly details: any) {
        super(`api error (${status}): ${JSON.stringify(details)}`);
    }
}

export function isApiError(error: any): error is ApiError {
    return error != null && error.status != null && error.details != null;
}