import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import {RootState} from '../store';

const initialState: {
    disableVisibility: boolean;
    disableValidation: boolean;
    useIdsInComponentTree: boolean;
    hideComponentTree: boolean;
    useTestMode: boolean;
    expandedTree: boolean;
    isDraggingTreeElement: boolean;
    showUserInput: boolean;
    warnDuplicateIds: boolean;
} = {
    disableVisibility: false,
    disableValidation: false,
    useIdsInComponentTree: false,
    hideComponentTree: false,
    useTestMode: false,
    expandedTree: false,
    isDraggingTreeElement: false,
    showUserInput: false,
    warnDuplicateIds: false,
};

const adminSettingsSlice = createSlice({
    name: 'adminSettings',
    initialState: {...initialState},
    reducers: {
        toggleVisibility: (state, _: PayloadAction<void>) => {
            state.disableVisibility = !state.disableVisibility;
        },
        toggleValidation: (state, _: PayloadAction<void>) => {
            state.disableValidation = !state.disableValidation;
        },
        toggleIdsInComponentTree: (state, _: PayloadAction<void>) => {
            state.useIdsInComponentTree = !state.useIdsInComponentTree;
        },
        toggleComponentTree: (state, _: PayloadAction<void>) => {
            state.hideComponentTree = !state.hideComponentTree;
        },
        toggleTestMode: (state, _: PayloadAction<void>) => {
            state.useTestMode = !state.useTestMode;
        },
        toggleExpandedTree: (state, _: PayloadAction<void>) => {
            state.expandedTree = !state.expandedTree;
        },
        setIsDraggingTreeElement: (state, action: PayloadAction<boolean>) => {
            state.isDraggingTreeElement = action.payload;
        },
        toggleShowUserInput: (state, _: PayloadAction<void>) => {
            state.showUserInput = !state.showUserInput;
        },
        toggleWarnDuplicateIds: (state, _: PayloadAction<void>) => {
            state.warnDuplicateIds = !state.warnDuplicateIds;
        },
        resetAdminSettings: (state, _: PayloadAction<void>) => {
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
    toggleExpandedTree,
    setIsDraggingTreeElement,
    toggleShowUserInput,
    toggleWarnDuplicateIds,
    resetAdminSettings,
} = adminSettingsSlice.actions;

export const selectDisableVisibility = (state: RootState) => state.adminSettings.disableVisibility;
export const selectDisableValidation = (state: RootState) => state.adminSettings.disableValidation;
export const selectUseIdsInComponentTree = (state: RootState) => state.adminSettings.useIdsInComponentTree;
export const selectHideComponentTree = (state: RootState) => state.adminSettings.hideComponentTree;
export const selectUseTestMode = (state: RootState) => state.adminSettings.useTestMode;
export const selectExpandedTree = (state: RootState) => state.adminSettings.expandedTree;
export const selectIsDraggingTreeElement = (state: RootState) => state.adminSettings.isDraggingTreeElement;
export const selectShowUserInput = (state: RootState) => state.adminSettings.showUserInput;
export const selectWarnDuplicateIds = (state: RootState) => state.adminSettings.warnDuplicateIds;

export const adminSettingsReducer = adminSettingsSlice.reducer;
