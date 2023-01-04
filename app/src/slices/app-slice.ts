import {createAsyncThunk, createSlice, PayloadAction} from '@reduxjs/toolkit'
import {ApplicationService} from '../services/application.service';
import {Application} from '../models/application';
import {CodeService} from '../services/code.service';
import {RootState} from '../store';

const initialState: {
    loadedApplication?: Application;
    applicationLoadFailed?: boolean;
} = {};

export const fetchApplicationById = createAsyncThunk(
    'app/fetchApplicationById',
    async (id: number, _) => {
        try {
            await CodeService.loadCode(id);
        } catch (err) {
            // TODO: Handle code not existent
        }
        return await ApplicationService.retrieve(id);
    }
);

export const fetchApplicationBySlug = createAsyncThunk(
    'app/fetchApplicationBySlug',
    async (req: { slug: string, version: string }, _) => {
        const application = await ApplicationService.retrieveBySlug(req.slug, req.version);
        try {
            await CodeService.loadCode(application.id);
        } catch (err) {
            // TODO: Handle code not existent
        }
        return application;
    }
);

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        clearAppModel: (state, _: PayloadAction<void>) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = false;
        },

        setAppModel: (state, action: PayloadAction<Application>) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
        },

        updateAppModel: (state, action: PayloadAction<Application>) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
            ApplicationService.update(action.payload.id, action.payload);
        },
    },
    extraReducers: (builder) => {
        builder.addCase(fetchApplicationById.fulfilled, (state, action) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
        });
        builder.addCase(fetchApplicationById.rejected, (state, _) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = true;
        });

        builder.addCase(fetchApplicationBySlug.fulfilled, (state, action) => {
            state.loadedApplication = action.payload;
            state.applicationLoadFailed = false;
        });
        builder.addCase(fetchApplicationBySlug.rejected, (state, _) => {
            state.loadedApplication = undefined;
            state.applicationLoadFailed = true;
        });
    },
});

export const {
    clearAppModel,
    setAppModel,
    updateAppModel,
} = appSlice.actions;

export const selectLoadedApplication = (state: RootState) => state.app.loadedApplication;
export const selectApplicationLoadFailed = (state: RootState) => state.app.applicationLoadFailed;

export const appReducer = appSlice.reducer;
