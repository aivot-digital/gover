import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import type {SystemSetupDTO} from '../modules/system/dtos/system-setup-dto';
import type {RootState} from '../store.staff';

export enum SnackbarSeverity {
    Info = 'info',
    Success = 'success',
    Warning = 'warning',
    Error = 'error',
}

export enum SnackbarType {
    AutoHiding = 'autoHiding',
    Loading = 'loading',
    Dismissable = 'dismissable',
    Permanent = 'permanent',
}

interface SnackbarMessage {
    key: string; // Einzigartiger Schlüssel für jede Nachricht
    message: string;
    severity: SnackbarSeverity;
    type: SnackbarType;
}

interface LoadingMessage {
    message: string;
    estimatedTime: number;
    blocking: boolean;
}

export type ShellLoadingMessage = LoadingMessage;

export interface ErrorMessage {
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
    snackbars: SnackbarMessage[];
    lastLoadingStartedAt?: number;
    loading?: LoadingMessage;
    error?: ErrorMessage;
    setup?: SystemSetupDTO;
    minimizeDrawer: boolean;
    showSearchDialog: boolean;
    showAboutGoverDialog: boolean;
}

const initialState: ShellState = {
    status: ShellStatus.Loading,
    snackbars: [],
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
            if (action.payload != null) {
                state.lastLoadingStartedAt = new Date().getTime();
            } else {
                state.lastLoadingStartedAt = undefined;
            }
        },
        clearLoadingMessage(state) {
            state.loading = undefined;
            state.lastLoadingStartedAt = undefined;
        },
        setErrorMessage(state, action: PayloadAction<ErrorMessage | undefined>) {
            state.error = action.payload;
        },
        addSnackbarMessage(state, action: PayloadAction<SnackbarMessage>) {
            // If key exists, update the message, else add new
            const existingIndex = state.snackbars.findIndex(msg => msg.key === action.payload.key);
            if (existingIndex !== -1) {
                state.snackbars[existingIndex] = action.payload;
            } else {
                state.snackbars.push(action.payload);
            }
        },
        removeSnackbarMessage(state, action: PayloadAction<string>) {
            state.snackbars = state.snackbars.filter(msg => msg.key !== action.payload);
        },
    },
});

export const {
    setStatus,
    setSetup,
    setMinimizeDrawer,
    setShowSearchDialog,
    setShowAboutGoverDialog,
    setLoadingMessage,
    clearLoadingMessage,
    setErrorMessage,
    addSnackbarMessage,
    removeSnackbarMessage,
} = shellSlice.actions;

export const selectStatus = (state: RootState) => state.shell.status;
export const selectSetup = (state: RootState) => state.shell.setup;
export const selectTheme = (state: RootState) => state.shell.setup?.providerTheme;
export const selectMinimizeDrawer = (state: RootState) => state.shell.minimizeDrawer;
export const selectShowSearchDialog = (state: RootState) => state.shell.showSearchDialog;
export const selectShowAboutGoverDialog = (state: RootState) => state.shell.showAboutGoverDialog;
export const selectLoadingMessage = (state: RootState) => state.shell.loading;
export const selectIsLoading = (state: RootState) => state.shell.loading != null;
export const selectErrorMessage = (state: RootState) => state.shell.error;
export const selectSnackbarMessages = (state: RootState) => state.shell.snackbars;

export const shellReducer = shellSlice.reducer;
