import {createAsyncThunk, createSlice} from '@reduxjs/toolkit'
import {SystemConfigsService} from '../services/system-configs.service';
import {RootState} from '../store';

const initialState: {
    [key: string]: string;
} = {};

export const fetchSystemConfig = createAsyncThunk(
    'systemConfig/fetchSystemConfig',
    async (_) => {
        return await SystemConfigsService.list();
    }
);

export const systemConfigSlice = createSlice({
    name: 'systemConfig',
    initialState,
    reducers: {},
    extraReducers: (builder) => {
        builder.addCase(fetchSystemConfig.fulfilled, (state, action) => {
            action.payload._embedded.systemConfigs.forEach(config => {
                state[config.key] = config.value;
            });
        });
    },
});

export const selectSystemConfig = (state: RootState) => state.systemConfig;
export const selectSystemConfigValue = (key: string) => (state: RootState) => state.systemConfig[key];

export const systemConfigReducer = systemConfigSlice.reducer;
