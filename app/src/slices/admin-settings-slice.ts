import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import {RootState} from '../store';

export interface AdminSettingsState {
    disableVisibility: boolean;
    disableValidation: boolean;
    useIdsInComponentTree: boolean;
    hideComponentTree: boolean;
    useTestMode: boolean;
    isDraggingTreeElement: boolean;
    showUserInput: boolean;
    warnDuplicateIds: boolean;
    showDebugOutput: boolean;
}

const initialState: AdminSettingsState = {
    disableVisibility: false,
    disableValidation: false,
    useIdsInComponentTree: false,
    hideComponentTree: false,
    useTestMode: false,
    isDraggingTreeElement: false,
    showUserInput: false,
    warnDuplicateIds: false,
    showDebugOutput: false,
};

const adminSettingsSlice = createSlice({
    name: 'adminSettings',
    initialState: {...initialState},
    reducers: {
        toggleVisibility: (state) => {
            state.disableVisibility = !state.disableVisibility;
        },
        toggleValidation: (state) => {
            state.disableValidation = !state.disableValidation;
        },
        toggleIdsInComponentTree: (state) => {
            state.useIdsInComponentTree = !state.useIdsInComponentTree;
        },
        toggleComponentTree: (state) => {
            state.hideComponentTree = !state.hideComponentTree;
        },
        toggleTestMode: (state) => {
            state.useTestMode = !state.useTestMode;
        },
        setIsDraggingTreeElement: (state, action: PayloadAction<boolean>) => {
            state.isDraggingTreeElement = action.payload;
        },
        toggleShowUserInput: (state) => {
            state.showUserInput = !state.showUserInput;
        },
        toggleWarnDuplicateIds: (state) => {
            state.warnDuplicateIds = !state.warnDuplicateIds;
        },
        toggleShowDebugOutput: (state) => {
            state.showDebugOutput = !state.showDebugOutput;
        },
        resetAdminSettings: (state) => {
            for (const key of Object.keys(initialState)) {
                (state as any)[key] = (initialState as any)[key];
            }
        },
    },
});

export const {
    toggleVisibility,
    toggleValidation,
    toggleIdsInComponentTree,
    toggleComponentTree,
    toggleTestMode,
    setIsDraggingTreeElement,
    toggleShowUserInput,
    toggleWarnDuplicateIds,
    toggleShowDebugOutput,
    resetAdminSettings,
} = adminSettingsSlice.actions;

export const selectDisableVisibility = (state: RootState) => state.adminSettings.disableVisibility;
export const selectDisableValidation = (state: RootState) => state.adminSettings.disableValidation;
export const selectUseIdsInComponentTree = (state: RootState) => state.adminSettings.useIdsInComponentTree;
export const selectHideComponentTree = (state: RootState) => state.adminSettings.hideComponentTree;
export const selectUseTestMode = (state: RootState) => state.adminSettings.useTestMode;
export const selectIsDraggingTreeElement = (state: RootState) => state.adminSettings.isDraggingTreeElement;
export const selectShowUserInput = (state: RootState) => state.adminSettings.showUserInput;
export const selectWarnDuplicateIds = (state: RootState) => state.adminSettings.warnDuplicateIds;
export const selectShowDebugOutput = (state: RootState) => state.adminSettings.showDebugOutput;

export const adminSettingsReducer = adminSettingsSlice.reducer;
