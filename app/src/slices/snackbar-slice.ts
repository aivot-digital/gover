import {createSlice, PayloadAction, createAsyncThunk} from '@reduxjs/toolkit';
import {AlertColor} from '@mui/material';

const initialState: {
    open?: Boolean;
    message?: string;
    severity?: AlertColor;
} = {};

const snackbarSlice = createSlice({
    name: 'snackbar',
    initialState,
    reducers: {
        showSuccessSnackbar: (state, action: PayloadAction<string>) => {
            state.open = true;
            state.message = action.payload;
            state.severity = 'success';
        },
        showErrorSnackbar: (state, action: PayloadAction<string>) => {
            state.open = true;
            state.message = action.payload;
            state.severity = 'error';
        },
        hideSnackbar: (state, _: PayloadAction<void>) => {
            state.open = false;
        },
        resetSnackbarContent: (state, _: PayloadAction<void>) => {
            state.open = false;
            state.message = undefined;
            state.severity = undefined;
        },
    },
});

export const {
    showSuccessSnackbar,
    showErrorSnackbar,
    hideSnackbar,
    resetSnackbarContent,
} = snackbarSlice.actions;

export const resetSnackbar = createAsyncThunk(
    'snackbar/resetSnackbar',
    async (_, { dispatch }) => {
        dispatch(hideSnackbar());
        await new Promise((resolve) => setTimeout(resolve, 300));
        dispatch(resetSnackbarContent());
    }
);

export const snackbarReducer = snackbarSlice.reducer;
