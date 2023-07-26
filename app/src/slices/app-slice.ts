import {createAsyncThunk, createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {ApplicationService} from '../services/application-service';
import {type RootState} from '../store';
import {type Application} from '../models/entities/application';

export enum MetaDialog {
    Privacy = 'privacy',
    Imprint = 'imprint',
    Accessibility = 'accessibility',
    Help = 'help',
}

const initialState: {
    loadedApplication?: Application;
    applicationLoadFailed?: boolean;
    applicationSaveFailed?: boolean;

    showMetaDialog?: MetaDialog;
} = {};

export const fetchApplicationById = createAsyncThunk(
    'app/fetchApplicationById',
    async (id: number, _) => {
        return await ApplicationService.retrieve(id);
    },
);

export const fetchApplicationBySlug = createAsyncThunk(
    'app/fetchApplicationBySlug',
    async (req: {slug: string, version: string}, _) => {
        return await ApplicationService.retrievePublic(req.slug, req.version);
    },
);

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        clearAppModel: (state, _: PayloadAction<void>) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = false;
            state.applicationSaveFailed = false;
            state.showMetaDialog = undefined;
        },

        setAppModel: (state, action: PayloadAction<Application>) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
            state.applicationSaveFailed = false;
        },

        updateAppModel: (state, action: PayloadAction<Application>) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
            ApplicationService
                .update(action.payload.id, action.payload)
                .catch((err) => {
                    console.error(err);
                    state.applicationSaveFailed = true;
                });
        },

        showMetaDialog: (state, action: PayloadAction<MetaDialog | undefined>) => {
            state.showMetaDialog = action.payload;
        },
    },
    extraReducers: (builder) => {
        builder.addCase(fetchApplicationById.fulfilled, (state, action) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
            state.applicationSaveFailed = false;
        });
        builder.addCase(fetchApplicationById.rejected, (state, _) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = true;
            state.applicationSaveFailed = false;
        });

        builder.addCase(fetchApplicationBySlug.fulfilled, (state, action) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
            state.applicationSaveFailed = false;
        });
        builder.addCase(fetchApplicationBySlug.rejected, (state, _) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = true;
            state.applicationSaveFailed = false;
        });
    },
});

export const {
    clearAppModel,
    setAppModel,
    updateAppModel,
    showMetaDialog,
} = appSlice.actions;

export const selectLoadedApplication = (state: RootState) => state.app.loadedApplication;
export const selectApplicationLoadFailed = (state: RootState) => state.app.applicationLoadFailed;
export const selectApplicationSaveFailed = (state: RootState) => state.app.applicationSaveFailed;

export const appReducer = appSlice.reducer;
