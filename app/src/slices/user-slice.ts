import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store.staff';
import {User} from '../modules/users/models/user';
import {VDepartmentMembershipWithDetailsEntityWithRoles} from '../modules/departments/entities/v-department-membership-with-details-entity';
import {VDepartmentUserRoleAssignmentWithDetailsEntity} from '../modules/departments/entities/v-department-user-role-assignment-with-details-entity';

interface UserState {
    user: User | undefined;
    memberships: VDepartmentMembershipWithDetailsEntityWithRoles[] | undefined;
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
        setMemberships: (state, action: PayloadAction<VDepartmentMembershipWithDetailsEntityWithRoles[]>) => {
            state.memberships = action.payload;
        },
    },
});

export const {
    setUser,
    setMemberships,
} = userSlice.actions;

export const selectUser = (state: RootState): User | undefined => state.user.user;
export const selectMemberships = (state: RootState): VDepartmentMembershipWithDetailsEntityWithRoles[] | undefined => state.user.memberships;
export const selectHasMemberships = (departmentId: number, permission: keyof VDepartmentUserRoleAssignmentWithDetailsEntity) => (state: RootState): boolean => state.user.memberships != null && state.user.memberships.some(mem => mem.departmentId === departmentId && mem.roles.some(role => role[permission] === true));

export const userReducer = userSlice.reducer;
