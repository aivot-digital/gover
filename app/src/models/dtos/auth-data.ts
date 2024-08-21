export interface AuthData {
    accessToken?: AuthDataAccessToken;
    refreshToken?: AuthDataRefreshToken;
}

export interface AuthDataAccessToken {
    token: string;
    expires: number;
}

export interface AuthDataRefreshToken {
    token: string;
    expires: number;
    idToken: string;
}