export interface HttpExchanges {
    exchanges: HttpExchange[];
}

export interface HttpExchange {
    timestamp: string;
    request: {
        uri: string;
        method: string;
        headers: {
            [key: string]: string[];
        };
    };
    response: {
        status: number;
        headers: {
            [key: string]: string[];
        };
    };
    timeTaken: string;
}