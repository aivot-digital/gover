import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store.staff';
import {User} from '../modules/users/models/user';
import {DepartmentMembershipWithRoles} from '../modules/departments/dtos/department-membership-response-dto';

interface UserState {
    user: User | undefined;
    memberships: DepartmentMembershipWithRoles[] | undefined;
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
        setMemberships: (state, action: PayloadAction<DepartmentMembershipWithRoles[]>) => {
            state.memberships = action.payload;
        },
    },
});

export const {
    setUser,
    setMemberships,
} = userSlice.actions;

export const selectUser = (state: RootState): User | undefined => state.user.user;
export const selectMemberships = (state: RootState): DepartmentMembershipWithRoles[] | undefined => state.user.memberships;

export const userReducer = userSlice.reducer;
