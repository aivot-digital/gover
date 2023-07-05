import {createAsyncThunk, createSlice} from '@reduxjs/toolkit'
import {UsersService} from '../services/users-service';
import {LocalStorageService} from '../services/local-storage-service';
import {RootState} from '../store';
import {LocalstorageKey} from "../data/localstorage-key";
import {User} from "../models/entities/user";
import {DepartmentMembership} from "../models/entities/department-membership";
import {DepartmentMembershipsService} from "../services/department-memberships-service";

const initialState: {
    user?: User;
    memberships?: DepartmentMembership[];
} = {};

export const refreshUser = createAsyncThunk(
    'user/refreshUser',
    async () => {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);

        if (jwt == null) {
            throw new Error('no stored used');
        }

        return await UsersService.getProfile();
    }
);

export const refreshMemberships = createAsyncThunk(
    'user/refreshMemberships',
    async (user: User) => {
        return await DepartmentMembershipsService.list({user: user.id});
    }
);

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder.addCase(refreshUser.fulfilled, (state, action) => {
            state.user = action.payload;
        });
        builder.addCase(refreshUser.rejected, (state, _) => {
            state.user = undefined;
        });

        builder.addCase(refreshMemberships.fulfilled, (state, action) => {
            state.memberships = action.payload;
        });
        builder.addCase(refreshMemberships.rejected, (state, _) => {
            state.memberships = undefined;
        });
    },
});

export const selectUser = (state: RootState) => state.user.user;
export const selectMemberships = (state: RootState) => state.user.memberships;

export const userReducer = userSlice.reducer;
