import {createAsyncThunk, createSlice, PayloadAction} from '@reduxjs/toolkit'
import {UsersService} from '../services/users-service';
import {LocalStorageService} from '../services/local-storage-service';
import {RootState} from '../store';
import {AuthState} from "../data/auth-state";
import {LocalstorageKey} from "../data/localstorage-key";


const initialState: {
    jwtContent?: string;
    authenticationState: AuthState;
} = {
    jwtContent: LocalStorageService.loadString(LocalstorageKey.JWT) ?? undefined,
    authenticationState: LocalStorageService.hasStored(LocalstorageKey.JWT) ? AuthState.Authenticated : AuthState.NotInitialized,
};

export const authenticate = createAsyncThunk(
    'auth/authenticate',
    async (credentials: { email: string; password: string }, _) => {
        return await UsersService.login(credentials.email, credentials.password);
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState,
    reducers: {
        logout: (state, _: PayloadAction<void>) => {
            state.jwtContent = undefined;
            state.authenticationState = AuthState.NotInitialized;
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        },
    },
    extraReducers: (builder) => {
        builder.addCase(authenticate.fulfilled, (state, action) => {
            state.jwtContent = action.payload.jwtToken;
            state.authenticationState = AuthState.Authenticated;
            LocalStorageService.storeString(LocalstorageKey.JWT, action.payload.jwtToken);
        });
        builder.addCase(authenticate.rejected, (state, _) => {
            state.jwtContent = undefined;
            state.authenticationState = AuthState.AuthenticationFailed;
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        });
    },
});

export const {
    logout,
} = authSlice.actions;

export const selectAuthenticationState = (state: RootState) => state.auth.authenticationState;

export const authReducer = authSlice.reducer;
