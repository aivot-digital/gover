import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store.staff';
import {type User} from '../modules/users/models/user';
import {
    type VDepartmentMembershipWithDetailsEntity,
} from '../modules/departments/entities/v-department-membership-with-details-entity';
import {type Permission} from '../data/permissions/permission';
import {type PermissionSet} from '../modules/permissions/models/permission-set';

interface UserState {
    user: User | undefined;
    memberships: VDepartmentMembershipWithDetailsEntity[] | undefined;
    permissions: PermissionSet | undefined;
}

const initialState: UserState = {
    user: undefined,
    memberships: undefined,
    permissions: undefined,
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
        setMemberships: (state, action: PayloadAction<VDepartmentMembershipWithDetailsEntity[]>) => {
            state.memberships = action.payload;
        },
        setPermissions: (state, action: PayloadAction<PermissionSet | undefined>) => {
            state.permissions = action.payload;
        },
    },
});

export const {
    setUser,
    setMemberships,
} = userSlice.actions;

export const selectUser = (state: RootState): User | undefined => state.user.user;
export const selectMemberships = (state: RootState): VDepartmentMembershipWithDetailsEntity[] | undefined => state.user.memberships;
export const selectPermissions = (state: RootState): PermissionSet | undefined => state.user.permissions;
export const selectHasMemberships = (departmentId: number, permission: Permission) => {
    return (state: RootState): boolean => {
        return state
            .user
            .memberships
            ?.some((mem) => {
                return mem.departmentId === departmentId &&
                    mem.domainRoles.some((role) => role.permissions.includes(permission));
            }) ?? false;
    };
};

export const userReducer = userSlice.reducer;
