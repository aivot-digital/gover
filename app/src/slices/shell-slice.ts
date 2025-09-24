import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import type {AlertColor} from '@mui/material';
import type {SystemSetupDTO} from '../modules/system/dtos/system-setup-dto';
import type {RootState} from '../store';

interface SnackbarMessage {
    key: string; // Einzigartiger Schlüssel für jede Nachricht
    message: string;
    severity: AlertColor;
}

interface LoadingMessage {
    message: string;
    estimatedTime: number;
    blocking: boolean;
}

interface ErrorMessage {
    type: 'not-found' | 'forbidden' | 'unknown';
    message?: string;
}

export enum ShellStatus {
    Loading,
    Login,
    Offline,
    Ready,
}


interface ShellState {
    status: ShellStatus;
    snackbars?: SnackbarMessage[];
    loading?: LoadingMessage;
    error?: ErrorMessage;
    setup?: SystemSetupDTO;
    maximizeDrawer: boolean;
    showSearchDialog: boolean;
}

const initialState: ShellState = {
    status: ShellStatus.Loading,
    maximizeDrawer: localStorage.getItem('maximizeDrawer') != null,
    showSearchDialog: false,
};

const shellSlice = createSlice({
    name: 'shell',
    initialState,
    reducers: {
        setStatus(state, action: PayloadAction<ShellStatus>) {
            state.status = action.payload;
        },
        setSetup(state, action: PayloadAction<SystemSetupDTO>) {
            state.setup = action.payload;
        },
        setMaximizeDrawer(state, action: PayloadAction<boolean>) {
            state.maximizeDrawer = action.payload;
            if (action.payload) {
                localStorage.setItem('maximizeDrawer', 'true');
            } else {
                localStorage.removeItem('maximizeDrawer');
            }
        },
        setShowSearchDialog(state, action: PayloadAction<boolean>) {
            state.showSearchDialog = action.payload;
        },
        setLoadingMessage(state, action: PayloadAction<LoadingMessage | undefined>) {
            state.loading = action.payload;
        },
    },
});

export const {
    setStatus,
    setSetup,
    setMaximizeDrawer,
    setShowSearchDialog,
    setLoadingMessage,
} = shellSlice.actions;

export const selectStatus = (state: RootState) => state.shell.status;
export const selectSetup = (state: RootState) => state.shell.setup;
export const selectTheme = (state: RootState) => state.shell.setup?.providerTheme;
export const selectMaximizeDrawer = (state: RootState) => state.shell.maximizeDrawer;
export const selectShowSearchDialog = (state: RootState) => state.shell.showSearchDialog;
export const selectLoadingMessage = (state: RootState) => state.shell.loading;

export const shellReducer = shellSlice.reducer;
