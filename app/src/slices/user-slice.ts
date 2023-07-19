import {createAsyncThunk, createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {UsersService} from '../services/users-service';
import {LocalStorageService} from '../services/local-storage-service';
import {type RootState} from '../store';
import {LocalstorageKey} from '../data/localstorage-key';
import {type AnyUser, NewAnonymousUser, type User} from '../models/entities/user';
import {type DepartmentMembership} from '../models/entities/department-membership';
import {DepartmentMembershipsService} from '../services/department-memberships-service';

interface UserState {
    user: AnyUser | undefined;
    memberships: DepartmentMembership[];
}

const initialState: UserState = {
    user: undefined,
    memberships: [],
};

/*
export const authenticate = createAsyncThunk(
    'user/authenticate',
    async (credentials: Credentials, _): Promise<User> => {
        const auth = await UsersService.login(credentials.email, credentials.password);
        LocalStorageService.storeString(LocalstorageKey.JWT, auth.jwtToken);
        return await UsersService.getProfile();
    },
);
*/

export const refreshUser = createAsyncThunk(
    'user/refreshUser',
    async (): Promise<User> => {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);

        if (jwt == null) {
            return NewAnonymousUser();
        }

        return await UsersService.getProfile();
    },
);

export const refreshMemberships = createAsyncThunk(
    'user/refreshMemberships',
    async (user: User): Promise<DepartmentMembership[]> => {
        return await DepartmentMembershipsService.list({user: user.id});
    },
);

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUser: (state, action: PayloadAction<AnyUser>) => {
            state.user = action.payload;
        },
        setMemberships: (state, action: PayloadAction<DepartmentMembership[]>) => {
            state.memberships = action.payload;
        },
        logout: (state, _: PayloadAction) => {
            state.user = NewAnonymousUser();
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        },
    },
    extraReducers: (builder) => {
        builder.addCase(refreshUser.fulfilled, (state, action) => {
            state.user = action.payload;
        });
        builder.addCase(refreshUser.rejected, (state, _) => {
            state.user = NewAnonymousUser();
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        });

        builder.addCase(refreshMemberships.fulfilled, (state, action) => {
            state.memberships = action.payload;
        });
        builder.addCase(refreshMemberships.rejected, (state, _) => {
            state.user = NewAnonymousUser();
            state.memberships = [];
            LocalStorageService.clearItem(LocalstorageKey.JWT);
        });
    },
});

export const {
    setUser,
    setMemberships,
    logout,
} = userSlice.actions;

export const selectUser = (state: RootState): AnyUser | undefined => state.user.user;
export const selectMemberships = (state: RootState): DepartmentMembership[] => state.user.memberships;

export const userReducer = userSlice.reducer;
