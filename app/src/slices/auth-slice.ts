import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {AuthData, AuthDataAccessToken, AuthDataRefreshToken} from '../models/dtos/auth-data';
import {StorageScope, StorageService} from '../services/storage-service';
import {StorageKey} from '../data/storage-key';
import {AppDispatch, RootState} from '../store';
import {AppConfig} from '../app-config';
import {getUrlWithoutQuery} from '../utils/location-utils';


export interface AuthState {
    authData?: AuthData;
}

export const refreshAuthData = () => async (dispatch: AppDispatch, getState: () => RootState) => {
    const {auth} = getState();
    const {authData} = auth;

    if (authData == null) {
        return;
    }

    const {
        accessToken,
        refreshToken,
    } = authData;

    if (accessToken == null || refreshToken == null) {
        return;
    }

    if (accessToken.expires > Date.now()) {
        return;
    }

    const response = await window.fetch(`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/protocol/openid-connect/token`, {
        method: 'POST',
        body: new URLSearchParams({
            grant_type: 'refresh_token',
            client_id: AppConfig.staff.client,
            refresh_token: refreshToken.token,
        }),
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        },
    });

    if (response.status !== 200) {
        return;
    }

    const refreshedAuthData = await response.json() as AuthDataDto;

    dispatch(setAuthData(refreshedAuthData));

    return authDataFromDTO(refreshedAuthData);
};

const authSlice = createSlice({
    name: 'auth',
    initialState: {
        authData: {
            accessToken: StorageService.loadObject<AuthDataAccessToken>(StorageKey.AuthDataAccessToken) ?? undefined,
            refreshToken: StorageService.loadObject<AuthDataRefreshToken>(StorageKey.AuthDataRefreshToken) ?? undefined,
        },
    } as AuthState,
    reducers: {
        setAuthData: (state, action: PayloadAction<AuthDataDto>) => {
            const authData = authDataFromDTO(action.payload);

            state.authData = authData;

            StorageService.storeObject(StorageKey.AuthDataAccessToken, authData.accessToken, StorageScope.Local);
            StorageService.storeObject(StorageKey.AuthDataRefreshToken, authData.refreshToken, StorageScope.Local);
        },
        updateAuthDataFromLocalStorage: (state, action: PayloadAction<void>) => {
            const authData: AuthData = {
                accessToken: StorageService.loadObject<AuthDataAccessToken>(StorageKey.AuthDataAccessToken) ?? undefined,
                refreshToken: StorageService.loadObject<AuthDataRefreshToken>(StorageKey.AuthDataRefreshToken) ?? undefined,
            };
            state.authData = authData;
        },
        clearAuthData: (state, _: PayloadAction) => {
            state.authData = undefined;
            StorageService.clearItem(StorageKey.AuthDataAccessToken);
            StorageService.clearItem(StorageKey.AuthDataAccessToken);
        },
    },
});

export const {
    setAuthData,
    updateAuthDataFromLocalStorage,
    clearAuthData,
} = authSlice.actions;

export const selectAuthData = (state: { auth: AuthState }) => state.auth.authData;
export const selectLogoutLink = (state: { auth: AuthState }) => {
    const idToken = state.auth.authData?.refreshToken?.idToken;

    if (idToken == null) {
        return '#';
    }

    return `${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/protocol/openid-connect/logout?` + new URLSearchParams({
        id_token_hint: idToken,
        post_logout_redirect_uri: getUrlWithoutQuery(),
    }).toString();
};

export const authReducer = authSlice.reducer;

function authDataFromDTO(dto: AuthDataDto): AuthData {
    const accessToken: AuthDataAccessToken = {
        token: dto.access_token,
        expires: new Date().getTime() + dto.expires_in * 1000,
    };

    const refreshToken: AuthDataRefreshToken = {
        token: dto.refresh_token,
        expires: new Date().getTime() + dto.refresh_expires_in * 1000,
        idToken: dto.id_token,
    };

    return {
        accessToken: accessToken,
        refreshToken: refreshToken,
    };
}
