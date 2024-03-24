import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {type User} from '../models/entities/user';
import {type DepartmentMembership} from '../models/entities/department-membership';

interface UserState {
    user: User | undefined;
    memberships: DepartmentMembership[] | undefined;
}

const initialState: UserState = {
    user: undefined,
    memberships: undefined,
};


const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUser: (state, action: PayloadAction<User | undefined>) => {
            state.user = action.payload;
            if (action.payload == null) {
                state.memberships = [];
            }
        },
        setMemberships: (state, action: PayloadAction<DepartmentMembership[]>) => {
            state.memberships = action.payload;
        },
    },
});

export const {
    setUser,
    setMemberships,
} = userSlice.actions;

export const selectUser = (state: RootState): User | undefined => state.user.user;
export const selectMemberships = (state: RootState): DepartmentMembership[] | undefined => state.user.memberships;

export const userReducer = userSlice.reducer;
