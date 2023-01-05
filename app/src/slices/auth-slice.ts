import {createAsyncThunk, createSlice, PayloadAction} from '@reduxjs/toolkit'
import {UsersService} from '../services/users.service';
import {LocalStorageService} from '../services/local-storage.service';
import {RootState} from '../store';

const initialState: {
    jwtContent?: string;
    authenticationState: 'not-initialized' | 'is-authenticated' | 'authentication-failed';
} = {
    jwtContent: LocalStorageService.loadString('jwt') ?? undefined,
    authenticationState: LocalStorageService.loadString('jwt') ? 'is-authenticated' : 'not-initialized',
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
            state.authenticationState = 'not-initialized';
            LocalStorageService.clearItem('jwt');
        },
    },
    extraReducers: (builder) => {
        builder.addCase(authenticate.fulfilled, (state, action) => {
            state.jwtContent = action.payload.jwtToken;
            state.authenticationState = 'is-authenticated';
            LocalStorageService.storeString('jwt', action.payload.jwtToken);
        });
        builder.addCase(authenticate.rejected, (state, _) => {
            state.jwtContent = undefined;
            state.authenticationState = 'authentication-failed';
            LocalStorageService.clearItem('jwt');
        });
    },
});

export const {
    logout,
} = authSlice.actions;

export const selectAuthenticationState = (state: RootState) => state.auth.authenticationState;

export const authReducer = authSlice.reducer;
