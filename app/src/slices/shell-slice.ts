import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import type {AlertColor} from '@mui/material';
import type {SystemSetupDTO} from '../modules/system/dtos/system-setup-dto';
import type {RootState} from '../store.staff';

interface SnackbarMessage {
    key: string; // Einzigartiger Schlüssel für jede Nachricht
    message: string;
    severity: AlertColor;
    duration?: number; // Dauer in Millisekunden, optional, if 0 then persistent
}

interface LoadingMessage {
    message: string;
    estimatedTime: number;
    blocking: boolean;
}

interface ErrorMessage {
    status: number;
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
    minimizeDrawer: boolean;
    showSearchDialog: boolean;
    showAboutGoverDialog: boolean;
}

const initialState: ShellState = {
    status: ShellStatus.Loading,
    minimizeDrawer: localStorage.getItem('minimizeDrawer') != null,
    showSearchDialog: false,
    showAboutGoverDialog: false,
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
        setMinimizeDrawer(state, action: PayloadAction<boolean>) {
            state.minimizeDrawer = action.payload;
            if (action.payload) {
                localStorage.setItem('minimizeDrawer', 'true');
            } else {
                localStorage.removeItem('minimizeDrawer');
            }
        },
        setShowSearchDialog(state, action: PayloadAction<boolean>) {
            state.showSearchDialog = action.payload;
        },
        setShowAboutGoverDialog(state, action: PayloadAction<boolean>) {
            state.showAboutGoverDialog = action.payload;
        },
        setLoadingMessage(state, action: PayloadAction<LoadingMessage | undefined>) {
            state.loading = action.payload;
        },
        setErrorMessage(state, action: PayloadAction<ErrorMessage | undefined>) {
            state.error = action.payload;
        },
        addSnackbarMessage(state, action: PayloadAction<SnackbarMessage>) {
            if (!state.snackbars) {
                state.snackbars = [];
            }
            state.snackbars.push(action.payload);
        }
    },
});

export const {
    setStatus,
    setSetup,
    setMinimizeDrawer,
    setShowSearchDialog,
    setShowAboutGoverDialog,
    setLoadingMessage,
    setErrorMessage,
    addSnackbarMessage,
} = shellSlice.actions;

export const selectStatus = (state: RootState) => state.shell.status;
export const selectSetup = (state: RootState) => state.shell.setup;
export const selectTheme = (state: RootState) => state.shell.setup?.providerTheme;
export const selectMinimizeDrawer = (state: RootState) => state.shell.minimizeDrawer;
export const selectShowSearchDialog = (state: RootState) => state.shell.showSearchDialog;
export const selectShowAboutGoverDialog = (state: RootState) => state.shell.showAboutGoverDialog;
export const selectLoadingMessage = (state: RootState) => state.shell.loading;
export const selectErrorMessage = (state: RootState) => state.shell.error;

export const shellReducer = shellSlice.reducer;
