import {createSlice, type PayloadAction} from '@reduxjs/toolkit';


export interface IdentityState {
    identityId?: string;
}

const identitySlice = createSlice({
    name: 'identity',
    initialState: {} as IdentityState,
    reducers: {
        setIdentityId: (state, action: PayloadAction<string | undefined>) => {
            state.identityId = action.payload;
        },
    },
});

export const {
    setIdentityId,
} = identitySlice.actions;

export const selectIdentityId = (state: { identity: IdentityState }) => state.identity.identityId;

export const identityReducer = identitySlice.reducer;
