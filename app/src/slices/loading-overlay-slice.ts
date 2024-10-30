import {createAsyncThunk, createSlice, PayloadAction} from '@reduxjs/toolkit';
import {RootState} from '../store';

const initialState: {
    open?: Boolean;
    message?: string;
    started?: number;
} = {};

const loadingOverlaySlice = createSlice({
    name: 'loading-overlay',
    initialState,
    reducers: {
        showLoadingOverlay: (state, action: PayloadAction<string>) => {
            state.open = true;
            state.message = action.payload;
            state.started = new Date().getTime();
        },
        visuallyHideLoadingOverlay: (state, _: PayloadAction) => {
            state.open = false;
        },
        resetLoadingOverlayContent: (state, _: PayloadAction<void>) => {
            state.open = false;
            state.message = undefined;
            state.started = undefined;
        },
    },
});

export const {
    showLoadingOverlay,
    visuallyHideLoadingOverlay,
    resetLoadingOverlayContent,
} = loadingOverlaySlice.actions;


export const hideLoadingOverlayWithTimeout = createAsyncThunk<void, number, { state: RootState }>(
    'loading-overlay/hideLoadingOverlayWithTimeout',
    async (timeout, {dispatch, getState}) => {
        const now = new Date().getTime();
        const elapsed = now - (getState().loadingOverlay.started ?? 0);
        const remaining = Math.max(0, timeout - elapsed);
        setTimeout(() => {
            dispatch(hideLoadingOverlay());
        }, remaining);
    },
);

export const hideLoadingOverlay = createAsyncThunk(
    'loading-overlay/hideLoadingOverlay',
    async (_, { dispatch }) => {
        dispatch(visuallyHideLoadingOverlay());
        await new Promise((resolve) => setTimeout(resolve, 300));
        dispatch(resetLoadingOverlayContent());
    }
);

export const loadingOverlayReducer = loadingOverlaySlice.reducer;
