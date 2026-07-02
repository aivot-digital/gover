import {createSlice, type PayloadAction} from '@reduxjs/toolkit';

const initialState: {
    // ID of the dialog to show
    showDialog?: string;
} = {};

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        showDialog: (state, action: PayloadAction<string | undefined>) => {
            state.showDialog = action.payload;
        },
    },
});

export const {
    showDialog,
} = appSlice.actions;

export const appReducer = appSlice.reducer;
