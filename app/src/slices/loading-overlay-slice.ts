import {createAsyncThunk} from '@reduxjs/toolkit';
import {RootState} from '../store.staff';
import {setLoadingMessage} from './shell-slice';

export const showLoadingOverlay = (message: string) => {
    return setLoadingMessage({
        blocking: true,
        message: message,
        estimatedTime: 500,
    });
};

export const hideLoadingOverlayWithTimeout = createAsyncThunk<void, number, { state: RootState }>(
    'loading-overlay/hideLoadingOverlayWithTimeout',
    async (timeout, {dispatch, getState}) => {
        const now = new Date().getTime();
        const elapsed = now - (getState().shell.lastLoadingStartedAt ?? 0);
        const remaining = Math.max(0, timeout - elapsed);
        setTimeout(() => {
            dispatch(hideLoadingOverlay());
        }, remaining);
    },
);

export const hideLoadingOverlay = createAsyncThunk(
    'loading-overlay/hideLoadingOverlay',
    async (_, {dispatch}) => {
        await new Promise((resolve) => setTimeout(resolve, 300));
        dispatch(setLoadingMessage(undefined));
    },
);
