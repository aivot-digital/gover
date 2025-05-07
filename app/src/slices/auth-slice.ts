import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {AuthDataDto} from '../models/dtos/auth-data-dto';
import {AuthData, AuthDataAccessToken, AuthDataRefreshToken} from '../models/dtos/auth-data';
import {User} from '../models/entities/user';
import type {DepartmentMembership} from '../modules/departments/models/department-membership';
import {StorageScope, StorageService} from '../services/storage-service';
import {StorageKey} from '../data/storage-key';


export interface AuthState {
    authData?: AuthData;
    authenticatedUser?: User;
    authenticatedUserMemberships?: DepartmentMembership[];
}

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
            const accessToken: AuthDataAccessToken = {
                token: action.payload.access_token,
                expires: new Date().getTime() + action.payload.expires_in * 1000,
            };

            const refreshToken: AuthDataRefreshToken = {
                token: action.payload.refresh_token,
                expires: new Date().getTime() + action.payload.refresh_expires_in * 1000,
                idToken: action.payload.id_token,
            };

            state.authData = {
                accessToken: accessToken,
                refreshToken: refreshToken,
            };

            StorageService.storeObject(StorageKey.AuthDataAccessToken, accessToken, StorageScope.Local);
            StorageService.storeObject(StorageKey.AuthDataRefreshToken, refreshToken, StorageScope.Local);
        },
        setUser: (state, action: PayloadAction<User>) => {
            state.authenticatedUser = action.payload;
        },
        setUserMemberships: (state, action: PayloadAction<DepartmentMembership[]>) => {
            state.authenticatedUserMemberships = action.payload;
        },
        clearAuthData: (state, _: PayloadAction) => {
            state.authData = undefined;
            state.authenticatedUser = undefined;
            state.authenticatedUserMemberships = undefined;

            StorageService.clearItem(StorageKey.AuthDataAccessToken);
            StorageService.clearItem(StorageKey.AuthDataRefreshToken);
        },
    },
});

export const {
    setAuthData,
    clearAuthData,
    setUser,
    setUserMemberships,
} = authSlice.actions;

export const selectAuthData = (state: {auth: AuthState}) => state.auth.authData;

export const authReducer = authSlice.reducer;
