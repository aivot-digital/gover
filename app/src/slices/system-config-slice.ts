import {createAsyncThunk, createSlice} from '@reduxjs/toolkit';
import {SystemConfigsService} from '../services/system-configs-service';
import {RootState} from '../store';


export type SystemConfigMap = {
    [key: string]: string;
}

const initialState: SystemConfigMap = {};

export const fetchSystemConfig = createAsyncThunk(
    'systemConfig/fetchSystemConfig',
    async (_) => {
        return await SystemConfigsService.list();
    },
);

export const fetchPublicSystemConfig = createAsyncThunk(
    'systemConfig/fetchPublicSystemConfig',
    async (_) => {
        return await SystemConfigsService.listPublicSystemConfigs();
    },
);

export const systemConfigSlice = createSlice({
    name: 'systemConfig',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchSystemConfig.fulfilled, (state, action) => {
                action.payload.forEach(config => {
                    state[config.key] = config.value;
                });
            })
            .addCase(fetchPublicSystemConfig.fulfilled, (state, action) => {
                action.payload.forEach(config => {
                    state[config.key] = config.value;
                });
            });
    },
});

export const selectSystemConfig = (state: RootState) => state.systemConfig;
export const selectSystemConfigValue = (key: string) => (state: RootState) => state.systemConfig[key];

export const systemConfigReducer = systemConfigSlice.reducer;
