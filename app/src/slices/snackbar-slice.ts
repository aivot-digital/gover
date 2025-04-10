import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { AlertColor } from '@mui/material';

interface SnackbarMessage {
    key: string; // Einzigartiger Schlüssel für jede Nachricht
    message: string;
    severity: AlertColor;
}

interface SnackbarState {
    messages: SnackbarMessage[];
}

const initialState: SnackbarState = {
    messages: [],
};

const snackbarSlice = createSlice({
    name: 'snackbar',
    initialState,
    reducers: {
        showSuccessSnackbar: (state, action: PayloadAction<string>) => {
            state.messages.unshift({
                key: new Date().getTime().toString(),
                message: action.payload,
                severity: 'success',
            });
        },
        showErrorSnackbar: (state, action: PayloadAction<string>) => {
            state.messages.unshift({
                key: new Date().getTime().toString(),
                message: action.payload,
                severity: 'error',
            });
        },
        showInfoSnackbar: (state, action: PayloadAction<string>) => {
            state.messages.unshift({
                key: new Date().getTime().toString(),
                message: action.payload,
                severity: 'info',
            });
        },
        showWarningSnackbar: (state, action: PayloadAction<string>) => {
            state.messages.unshift({
                key: new Date().getTime().toString(),
                message: action.payload,
                severity: 'warning',
            });
        },
        removeSnackbar: (state, action: PayloadAction<string>) => {
            state.messages = state.messages.filter(msg => msg.key !== action.payload);
        },
        showLoadingSnackbar: (state, action: PayloadAction<string>) => {
            const key = 'loading-toast';
            const alreadyExists = state.messages.find(msg => msg.key === key);
            if (!alreadyExists) {
                state.messages.unshift({
                    key,
                    message: action.payload,
                    severity: 'info',
                });
            }
        },
        removeLoadingSnackbar: (state) => {
            state.messages = state.messages.filter(msg => msg.key !== 'loading-toast');
        },
    },
});

export const {
    showSuccessSnackbar,
    showErrorSnackbar,
    showInfoSnackbar,
    showWarningSnackbar,
    removeSnackbar,
    showLoadingSnackbar,
    removeLoadingSnackbar,
} = snackbarSlice.actions;

export const snackbarReducer = snackbarSlice.reducer;
