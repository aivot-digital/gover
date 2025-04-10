import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import type {RootState} from '../store';

export enum LogLevel {
    Debug,
    Info,
    Warning,
    Error,
}

export const LogLevelNames: Record<LogLevel, string> = {
    [LogLevel.Debug]: 'Debug',
    [LogLevel.Info]: 'Info',
    [LogLevel.Warning]: 'Warning',
    [LogLevel.Error]: 'Error',
};

export interface LogEntry {
    type: LogLevel;
    source: string;
    message: any;
    timestamp: number;
}

const initialState: {
    logLevel: LogLevel;
    entries: LogEntry[];
} = {
    logLevel: LogLevel.Warning,
    entries: [],
};

const loggingSlice = createSlice({
    name: 'logging',
    initialState: initialState,
    reducers: {
        setLogLevel: (state, action: PayloadAction<LogLevel>) => {
            state.logLevel = action.payload;
        },
        logDebug: (state, action: PayloadAction<Pick<LogEntry, 'source' | 'message'>>) => {
            state.entries = [
                ...state.entries,
                {
                    ...action.payload,
                    type: LogLevel.Debug,
                    timestamp: Date.now(),
                },
            ];
            if (state.logLevel <= LogLevel.Debug) {
                console.log(action.payload.source, action.payload.message);
            }
        },
        logInfo: (state, action: PayloadAction<Pick<LogEntry, 'source' | 'message'>>) => {
            state.entries = [
                ...state.entries,
                {
                    ...action.payload,
                    type: LogLevel.Info,
                    timestamp: Date.now(),
                },
            ];
            if (state.logLevel <= LogLevel.Info) {
                console.info(action.payload.source, action.payload.message);
            }
        },
        logWarning: (state, action: PayloadAction<Pick<LogEntry, 'source' | 'message'>>) => {
            state.entries = [
                ...state.entries,
                {
                    ...action.payload,
                    type: LogLevel.Warning,
                    timestamp: Date.now(),
                },
            ];
            if (state.logLevel <= LogLevel.Warning) {
                console.warn(action.payload.source, action.payload.message);
            }
        },
        logError: (state, action: PayloadAction<Pick<LogEntry, 'source' | 'message'>>) => {
            state.entries = [
                ...state.entries,
                {
                    ...action.payload,
                    type: LogLevel.Error,
                    timestamp: Date.now(),
                },
            ];
            if (state.logLevel <= LogLevel.Error) {
                console.error(action.payload.source, action.payload.message);
            }
        },
    },
});

export const {
    setLogLevel,
    logDebug,
    logInfo,
    logWarning,
    logError,
} = loggingSlice.actions;

export const selectLogLevel = (state: RootState) => state.logging.logLevel;
export const selectLogs = (minLogLevel: LogLevel) => (state: RootState) => {
    return state.logging.entries.filter(entry => entry.type >= minLogLevel);
};

export const loggingReducer = loggingSlice.reducer;
