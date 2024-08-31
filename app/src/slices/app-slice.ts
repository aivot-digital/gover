import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {type Form as Application, FormPublicProjection} from '../models/entities/form';
import type {CustomerInput} from "../models/customer-input";
import {CustomerInputErrors} from "../models/customer-input-errors";
import {CustomerInputService} from "../services/customer-input-service";
import {isFileUploadElementItem} from "../models/elements/form/input/file-upload-element";


const initialState: {
    loadedForm?: Application | FormPublicProjection;
    showDialog?: string;
    hasLoadedSavedCustomerInput: boolean;
    inputs: CustomerInput;
    errors: CustomerInputErrors;
    disabled: Record<string, boolean>;
} = {
    hasLoadedSavedCustomerInput: false,
    inputs: {},
    errors: {},
    disabled: {},
};

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        clearLoadedForm: (state, _: PayloadAction<void>) => {
            state.loadedForm = undefined;
            state.showDialog = undefined;
            state.hasLoadedSavedCustomerInput = false;
            state.inputs = {};
            state.errors = {};
        },

        updateLoadedForm: (state, action: PayloadAction<Application | FormPublicProjection>) => {
            state.loadedForm = action.payload;
        },

        showDialog: (state, action: PayloadAction<string | undefined>) => {
            state.showDialog = action.payload;
        },

        setHasLoadedSavedCustomerInput: (state, action: PayloadAction<boolean>) => {
            state.hasLoadedSavedCustomerInput = action.payload;
        },

        hydrateCustomerInput: (state, action: PayloadAction<CustomerInput>) => {
            const cleanedSaveData = {
                ...action.payload,
            };
            for (const key of Object.keys(cleanedSaveData)) {
                const val = cleanedSaveData[key];
                if (Array.isArray(val) && val.length > 0 && isFileUploadElementItem(val[0])) {
                    delete cleanedSaveData[key];
                }
            }
            state.inputs = cleanedSaveData;
            if (state.loadedForm != null) {
                CustomerInputService.storeCustomerInput(state.loadedForm, cleanedSaveData);
            }
        },

        clearCustomerInput: (state, _: PayloadAction<void>) => {
            state.inputs = {};
            state.disabled = {};
            state.errors = {};
            if (state.loadedForm != null) {
                CustomerInputService.cleanCustomerInput(state.loadedForm);
            }
        },

        updateCustomerInput: (state, action: PayloadAction<{key: string, value: any}>) => {
            const newInput = {
                ...state.inputs,
                [action.payload.key]: action.payload.value,
            }
            state.inputs = newInput
            if (state.loadedForm != null) {
                CustomerInputService.storeCustomerInput(state.loadedForm, newInput);
            }
        },

        clearErrors: (state, _: PayloadAction<void>) => {
            state.errors = {};
        },

        addError: (state, action: PayloadAction<{key: string, error: string;}>) => {
            state.errors[action.payload.key] = action.payload.error;
        },

        hydrateDisabled: (state, action: PayloadAction<Record<string, boolean>>) => {
            state.disabled = action.payload;
        },
        setDisabled: (state, action: PayloadAction<{key: string, value: boolean}>) => {
            state.disabled[action.payload.key] = action.payload.value;
        },
        clearDisabled: (state, _: PayloadAction<void>) => {
            state.disabled = {};
        },
    },
});

export const {
    clearLoadedForm,
    updateLoadedForm,
    showDialog,
    setHasLoadedSavedCustomerInput,
    hydrateCustomerInput,
    clearCustomerInput,
    updateCustomerInput,
    clearErrors,
    addError,
    hydrateDisabled,
    setDisabled,
    clearDisabled,
} = appSlice.actions;

export const selectLoadedForm = (state: RootState) => state.app.loadedForm;
export const selectCustomerInputValue = (key: string) => (state: RootState) => state.app.inputs[key];
export const selectCustomerInputError = (key: string) => (state: RootState) => state.app.errors[key];
export const selectHasLoadedSavedCustomerInput = () => (state: RootState) => state.app.hasLoadedSavedCustomerInput;
export const selectDisabled = (key: string) => (state: RootState) => state.app.disabled[key];

export const appReducer = appSlice.reducer;
