export interface ApiError {
    status: number;
    message: string;
    details: any | null | undefined;
    displayableToUser: boolean;
}

export async function createApiError(response: Response): Promise<ApiError> {
    // Try to parse the error response as json
    let details: any = null;
    try {
        const jsonBody = await response.json();
        if (isApiError(jsonBody)) {
            // Overwrite the status code of the api error, to make sure to match the status code with the response status code.
            return {
                ...jsonBody,
                status: response.status,
            };
        } else {
            details = jsonBody;
        }
    } catch (ignored) {
        // failed to parse as json try text
        try {
            details = await response.text();
        } catch (error) {
            details = `No details provided. The error was not parseable as json or text. Error: ${error}`;
        }
    }

    return {
        status: response.status,
        message: response.statusText,
        details: details,
        displayableToUser: false,
    };
}

export function isApiError(error: any): error is ApiError {
    return (
        error != null &&
        error.status != null &&
        typeof error.status === 'number' &&
        error.message != null &&
        typeof error.message === 'string' &&
        typeof error.displayableToUser === 'boolean'
    );
}