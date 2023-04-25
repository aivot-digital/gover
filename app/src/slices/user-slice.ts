import {createAsyncThunk, createSlice} from '@reduxjs/toolkit'
import {UsersService} from '../services/users.service';
import {LocalStorageService} from '../services/local-storage.service';
import {RootState} from '../store';
import {LocalstorageKey} from "../data/localstorage-key";
import {User} from "../models/entities/user";

const initialState: {
    user?: User;
} = {};

export const refreshUser = createAsyncThunk(
    'user/refresh',
    async () => {
        const jwt = LocalStorageService.loadString(LocalstorageKey.JWT);

        if (jwt == null) {
            throw new Error('no stored used');
        }

        return await UsersService.getProfile();
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
    },
});

export const selectUser = (state: RootState) => state.user.user;

export const userReducer = userSlice.reducer;
