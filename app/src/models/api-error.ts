export class ApiError extends Error {
    constructor(public readonly status: number, public readonly details: string) {
        super('api error ' + details);
    }
}