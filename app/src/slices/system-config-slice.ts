import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {SystemConfigResponseDto as SystemConfig} from '../modules/configs/dtos/system-config-response-dto';


export type SystemConfigMap = Record<string, string>;

const initialState: SystemConfigMap = {};

export const systemConfigSlice = createSlice({
    name: 'systemConfig',
    initialState,
    reducers: {
        setSystemConfigs: (state, action: PayloadAction<SystemConfig[]>) => {
            action.payload.forEach((config) => {
                state[config.key] = config.value;
            });
        },
        setSystemConfig: (state, action: PayloadAction<SystemConfig>) => {
            state[action.payload.key] = action.payload.value;
        },
    },
});

export const {
    setSystemConfigs,
    setSystemConfig,
} = systemConfigSlice.actions;

export const selectSystemConfig = (state: RootState) => state.systemConfig;
export const selectSystemConfigValue = (key: string) => (state: RootState) => state.systemConfig[key];
export const selectBooleanSystemConfigValue = (key: string) => (state: RootState) => state.systemConfig[key] === 'true';

export const systemConfigReducer = systemConfigSlice.reducer;
