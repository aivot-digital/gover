import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {AlertColor} from '@mui/material';

const initialState: {
    message?: string;
    severity?: AlertColor;
} = {};

const snackbarSlice = createSlice({
    name: 'snackbar',
    initialState,
    reducers: {
        showSuccessSnackbar: (state, action: PayloadAction<string>) => {
            state.message = action.payload;
            state.severity = 'success';
        },
        showErrorSnackbar: (state, action: PayloadAction<string>) => {
            state.message = action.payload;
            state.severity = 'error';
        },
        resetSnackbar: (state, _: PayloadAction<void>) => {
            state.message = undefined;
            state.severity = undefined;
        },
    },
});

export const {
    showSuccessSnackbar,
    showErrorSnackbar,
    resetSnackbar,
} = snackbarSlice.actions;

export const snackbarReducer = snackbarSlice.reducer;
