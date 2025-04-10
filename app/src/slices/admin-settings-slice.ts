import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {RootState} from '../store';
import {AnyElement} from '../models/elements/any-element';

export interface AdminSettingsState {
    disableVisibility: boolean;
    disableValidation: boolean;
    useIdsInComponentTree: boolean;
    hideComponentTree: boolean;
    useTestMode: boolean;
    draggingTreeElement: AnyElement | undefined;
    expandElementTree: undefined | 'expanded' | 'collapsed';
    warnDuplicateIds: boolean;
    disableAutoScrollForSteps: boolean;
    treeElementSearch?: string;
    devToolsTab?: number;
}

const initialState: AdminSettingsState = {
    disableVisibility: false,
    disableValidation: false,
    useIdsInComponentTree: false,
    hideComponentTree: false,
    useTestMode: false,
    draggingTreeElement: undefined,
    expandElementTree: undefined,
    warnDuplicateIds: false,
    disableAutoScrollForSteps: false,
    treeElementSearch: undefined,
    devToolsTab: undefined,
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
        setDraggingTreeElement: (state, action: PayloadAction<AnyElement | undefined>) => {
            state.draggingTreeElement = action.payload;
        },
        toggleWarnDuplicateIds: (state) => {
            state.warnDuplicateIds = !state.warnDuplicateIds;
        },
        toggleAutoScrollForSteps: (state) => {
            state.disableAutoScrollForSteps = !state.disableAutoScrollForSteps;
        },
        setTreeElementSearch: (state, action: PayloadAction<string | undefined>) => {
            state.treeElementSearch = action.payload;
        },
        resetAdminSettings: (state) => {
            for (const key of Object.keys(initialState)) {
                (state as any)[key] = (initialState as any)[key];
            }
        },
        setExpandElementTree: (state, action: PayloadAction<undefined | 'expanded' | 'collapsed'>) => {
            state.expandElementTree = action.payload;
        },
        setDevToolsTab: (state, action: PayloadAction<number | undefined>) => {
            state.devToolsTab = action.payload;
        },
    },
});

export const {
    toggleVisibility,
    toggleValidation,
    toggleIdsInComponentTree,
    toggleComponentTree,
    toggleTestMode,
    setDraggingTreeElement,
    toggleWarnDuplicateIds,
    toggleAutoScrollForSteps,
    resetAdminSettings,
    setTreeElementSearch,
    setExpandElementTree,
    setDevToolsTab,
} = adminSettingsSlice.actions;

export const selectDisableVisibility = (state: RootState) => state.adminSettings.disableVisibility;
export const selectUseIdsInComponentTree = (state: RootState) => state.adminSettings.useIdsInComponentTree;
export const selectUseTestMode = (state: RootState) => state.adminSettings.useTestMode;
export const selectIsDraggingTreeElement = (state: RootState) => state.adminSettings.draggingTreeElement != null;
export const selectDraggingTreeElement = (state: RootState) => state.adminSettings.draggingTreeElement;
export const selectWarnDuplicateIds = (state: RootState) => state.adminSettings.warnDuplicateIds;
export const selectDisableAutoScrollForSteps = (state: RootState) => state.adminSettings.disableAutoScrollForSteps;
export const selectTreeElementSearch = (state: RootState) => state.adminSettings.treeElementSearch;
export const selectDevToolsTab = (state: RootState) => state.adminSettings.devToolsTab;

export const adminSettingsReducer = adminSettingsSlice.reducer;
