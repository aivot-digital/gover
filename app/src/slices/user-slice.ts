import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import { UsersService } from '../services/users-service';
import { LocalStorageService } from '../services/local-storage-service';
import { type RootState } from '../store';
import { LocalstorageKey } from '../data/localstorage-key';
import { type AnonymousUser, type InvalidUser, type User } from '../models/entities/user';
import { type DepartmentMembership } from '../models/entities/department-membership';
import { DepartmentMembershipsService } from '../services/department-memberships-service';
import { type Credentials } from '../models/dtos/credentials';

interface UserState {
    user: User | AnonymousUser | InvalidUser | undefined;
    memberships: DepartmentMembership[];
}

const initialState: UserState = {
    user: undefined,
    memberships: [],
};

export const authenticate = createAsyncThunk(
    'user/authenticate',
    async (credentials: Credentials, _): Promise<User> => {
        const auth = await UsersService.login(credentials.email, credentials.password);
        LocalStorageService.storeString(LocalstorageKey.JWT, auth.jwtToken);
        return await UsersService.getProfile();
    },
);

export const refreshUser = createAsyncThunk(
    'user/refreshUser',
    async (): Promise<User> => {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);

        if (jwt == null) {
            return {
                id: -1,
                name: 'Anonymous',
                email: '',
                password: '',
                active: false,
                admin: false,
                created: '',
                updated: '',
            };
        }

        return await UsersService.getProfile();
    },
);

export const refreshMemberships = createAsyncThunk(
    'user/refreshMemberships',
    async (user: User): Promise<DepartmentMembership[]> => {
        return await DepartmentMembershipsService.list({ user: user.id });
    },
);

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        logout: (state, _: PayloadAction) => {
            state.user = {
                id: -1,
                name: 'Anonymous',
                email: '',
                password: '',
                active: false,
                admin: false,
                created: '',
                updated: '',
            };
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        },
    },
    extraReducers: (builder) => {
        builder.addCase(authenticate.fulfilled, (state, action) => {
            state.user = action.payload;
        });
        builder.addCase(authenticate.rejected, (state, _) => {
            state.user = {
                id: -2,
                name: 'Invalid',
                email: '',
                password: '',
                active: false,
                admin: false,
                created: '',
                updated: '',
            };
        });

        builder.addCase(refreshUser.fulfilled, (state, action) => {
            state.user = action.payload;
        });
        builder.addCase(refreshUser.rejected, (state, _) => {
            state.user = {
                id: -1,
                name: 'Anonymous',
                email: '',
                password: '',
                active: false,
                admin: false,
                created: '',
                updated: '',
            };
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        });

        builder.addCase(refreshMemberships.fulfilled, (state, action) => {
            state.memberships = action.payload;
        });
        builder.addCase(refreshMemberships.rejected, (state, _) => {
            state.user = {
                id: -1,
                name: 'Anonymous',
                email: '',
                password: '',
                active: false,
                admin: false,
                created: '',
                updated: '',
            };
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        });
    },
});

export const { logout } = userSlice.actions;

export const selectUser = (state: RootState): User | undefined => state.user.user;
export const selectMemberships = (state: RootState): DepartmentMembership[] => state.user.memberships;

export const userReducer = userSlice.reducer;
