import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {type Form as Application, FormPublicProjection} from '../models/entities/form';
import type {CustomerInput} from '../models/customer-input';
import {CustomerInputErrors} from '../models/customer-input-errors';
import {CustomerInputService} from '../services/customer-input-service';
import {isFileUploadElementItem} from '../models/elements/form/input/file-upload-element';
import {FormState} from '../models/dtos/form-state';
import {AnyElement} from '../models/elements/any-element';
import {collectReferences, Reference} from '../utils/build-references';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../utils/flatten-elements';
import {IdentityValue} from '../modules/identity/models/identity-value';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {prefillElements} from '../utils/prefill-elements';


const initialState: {
    // Future states of the loaded form. These get created when changes are undone
    futureLoadedForm: Array<Application | FormPublicProjection>,
    // The form that has been loaded
    loadedForm?: Application | FormPublicProjection;
    // Past states of the loaded form. These get created when changes are done
    pastLoadedForm: Array<Application | FormPublicProjection>,
    // Reference tree for the loaded form
    functionReferences?: Reference[];
    // A list of all elements in the form
    allElements?: AnyElement[];
    // A list of all elements in the form with their parents
    allElementsWithParents?: ElementWithParents[];

    // A queue of element ids, which have triggered a derivation
    derivationTriggerIdQueue: string[];

    // ID of the dialog to show
    showDialog?: string;

    // Whether the customer input has been loaded from the local storage
    hasLoadedSavedCustomerInput: boolean;
    // Inputs the customer has actively entered
    inputs: CustomerInput;
    // Values the customer has entered or were computes
    values: CustomerInput;
    // Errors for the customer input
    errors: CustomerInputErrors;
    // Record of disabled elements
    disabled: Record<string, boolean>; // TODO: Rename disabled to prefilled to make it clearer
    // Record of invisible elements
    visibilities: Record<string, boolean>;
    // Record of element overrides
    overrides: Record<string, AnyElement>;
} = {
    futureLoadedForm: [],
    pastLoadedForm: [],
    derivationTriggerIdQueue: [],
    hasLoadedSavedCustomerInput: false,
    inputs: {},
    values: {},
    errors: {},
    disabled: {},
    visibilities: {},
    overrides: {},
};

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        clearLoadedForm: (state, _: PayloadAction<void>) => {
            state.loadedForm = undefined;
            state.functionReferences = undefined;
            state.allElements = undefined;
            state.allElementsWithParents = undefined;
            state.derivationTriggerIdQueue = [];
            state.futureLoadedForm = [];
            state.pastLoadedForm = [];
            state.showDialog = undefined;
            state.hasLoadedSavedCustomerInput = false;
            state.inputs = {};
            state.values = {};
            state.errors = {};
            state.disabled = {};
            state.visibilities = {};
            state.overrides = {};
        },

        updateLoadedForm: (state, action: PayloadAction<Application | FormPublicProjection>) => {
            if (state.loadedForm != null) {
                state.pastLoadedForm.push(state.loadedForm);
                state.futureLoadedForm = [];
            }
            state.loadedForm = action.payload;
            state.functionReferences = collectReferences(state.loadedForm.root);
            state.allElements = flattenElements(state.loadedForm.root);
            state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.root, [], false);
        },

        undoLoadedForm: (state, _: PayloadAction<void>) => {
            if (state.pastLoadedForm.length > 0 && state.loadedForm != null) {
                state.futureLoadedForm.push(state.loadedForm);
                state.loadedForm = state.pastLoadedForm.pop();
                if (state.loadedForm != null) {
                    state.functionReferences = collectReferences(state.loadedForm.root);
                    state.allElements = flattenElements(state.loadedForm.root);
                    state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.root, [], false);
                } else {
                    state.functionReferences = undefined;
                    state.allElements = undefined;
                    state.allElementsWithParents = undefined;
                    state.derivationTriggerIdQueue = [];
                }
            }
        },

        redoLoadedForm: (state, _: PayloadAction<void>) => {
            if (state.futureLoadedForm.length > 0 && state.loadedForm != null) {
                state.pastLoadedForm.push(state.loadedForm);
                state.loadedForm = state.futureLoadedForm.pop();
                if (state.loadedForm != null) {
                    state.functionReferences = collectReferences(state.loadedForm.root);
                    state.allElements = flattenElements(state.loadedForm.root);
                    state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.root, [], false);
                } else {
                    state.functionReferences = undefined;
                    state.allElements = undefined;
                    state.allElementsWithParents = undefined;
                    state.derivationTriggerIdQueue = [];
                }
            }
        },

        clearLoadedFormHistory: (state, _: PayloadAction<void>) => {
            state.pastLoadedForm = [];
            state.futureLoadedForm = [];
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
            state.values = {};
            state.disabled = {};
            state.errors = {};
            state.visibilities = {};
            state.overrides = {};
            if (state.loadedForm != null) {
                CustomerInputService.cleanCustomerInput(state.loadedForm);
            }
        },

        updateCustomerInput: (state, action: PayloadAction<{ key: string, value: any }>) => {
            const newInput = {
                ...state.inputs,
                [action.payload.key]: action.payload.value,
            };
            state.inputs = newInput;
            state.values = {
                ...state.values,
                [action.payload.key]: undefined,
            };

            if (state.loadedForm != null) {
                CustomerInputService.storeCustomerInput(state.loadedForm, newInput);
            }
        },

        addError: (state, action: PayloadAction<{ key: string, error: string; }>) => {
            state.errors[action.payload.key] = action.payload.error;
        },
        clearErrors: (state, _: PayloadAction<void>) => {
            state.errors = {};
        },

        hydrateDisabled: (state, action: PayloadAction<Record<string, boolean>>) => {
            state.disabled = action.payload;
        },
        clearDisabled: (state, _: PayloadAction<void>) => {
            state.disabled = {};
        },

        clearVisibilities: (state, _: PayloadAction<void>) => {
            state.visibilities = {};
        },

        hydrateFromDerivation: (state, action: PayloadAction<FormState>) => {
            state.values = {
                ...state.values,
                ...action.payload.values,
            };
            state.visibilities = {
                ...state.visibilities,
                ...action.payload.visibilities,
            };
            state.errors = action.payload.errors; // Errors are always overwritten to prevent errors from future steps being carried over
            state.overrides = {
                ...state.overrides,
                ...action.payload.overrides,
            };
        },
        hydrateFromDerivationWithoutErrors: (state, action: PayloadAction<FormState>) => {
            state.values = {
                ...state.values,
                ...action.payload.values,
            };
            state.visibilities = {
                ...state.visibilities,
                ...action.payload.visibilities,
            };
            state.overrides = {
                ...state.overrides,
                ...action.payload.overrides,
            };
        },

        setFormState: (state, action: PayloadAction<{
            formState: FormState;
            options: {
                freshVisibilities?: boolean;
                ignoreErrors?: boolean;
            };
        }>) => {
            const {
                formState,
                options,
            } = action.payload;

            state.values = {
                ...state.values,
                ...formState.values,
            };

            if (options.freshVisibilities) {
                state.visibilities = {
                    ...formState.visibilities,
                };
            } else {
                state.visibilities = {
                    ...state.visibilities,
                    ...formState.visibilities,
                };
            }

            if (!options.ignoreErrors) {
                state.errors = formState.errors; // Errors are always overwritten to prevent errors from future steps being carried over
            }

            state.overrides = {
                ...state.overrides,
                ...formState.overrides,
            };
        },

        enqueueDerivationTriggerId: (state, action: PayloadAction<string>) => {
            state.derivationTriggerIdQueue.push(action.payload);
        },
        dequeueDerivationTriggerId: (state, action: PayloadAction<void>) => {
            const tmp = [...state.derivationTriggerIdQueue];
            tmp.shift();
            state.derivationTriggerIdQueue = tmp;
        },

        prefillElementsFromIdentityProvider: (state, action: PayloadAction<IdentityValue>) => {
            if (state.loadedForm == null) {
                return;
            }

            const prefilled = prefillElements(state.loadedForm, action.payload, state.inputs);
            const disabled = Object.keys(prefilled).reduce((acc, key) => {
                return {
                    ...acc,
                    [key]: true,
                }
            }, {});

            state.inputs = {
                ...state.inputs,
                ...prefilled,
            };
            state.disabled = disabled;
        },
    },
});

export const {
    clearLoadedForm,
    updateLoadedForm,
    undoLoadedForm,
    redoLoadedForm,
    clearLoadedFormHistory,
    showDialog,
    setHasLoadedSavedCustomerInput,
    hydrateCustomerInput,
    clearCustomerInput,
    updateCustomerInput,
    addError,
    clearErrors,
    clearVisibilities,
    hydrateDisabled,
    clearDisabled,
    hydrateFromDerivation,
    hydrateFromDerivationWithoutErrors,
    setFormState,
    enqueueDerivationTriggerId,
    dequeueDerivationTriggerId,
    prefillElementsFromIdentityProvider,
} = appSlice.actions;

export const selectLoadedForm = (state: RootState) => state.app.loadedForm;
export const selectPastLoadedForm = (state: RootState) => state.app.pastLoadedForm;
export const selectFutureLoadedForm = (state: RootState) => state.app.futureLoadedForm;
export const selectCustomerInputs = (state: RootState) => state.app.inputs;
export const selectCustomerInputValue = (key: string) => (state: RootState) => state.app.inputs[key];
export const selectCustomerInputError = (key: string) => (state: RootState) => state.app.errors[key];
export const selectHasLoadedSavedCustomerInput = () => (state: RootState) => state.app.hasLoadedSavedCustomerInput;
export const selectFunctionReferences = (state: RootState) => state.app.functionReferences;
export const selectAllElements = (state: RootState) => state.app.allElements;

export const selectDisabled = (key: string) => (state: RootState) => state.app.disabled[key];
export const selectVisibilies = (state: RootState) => state.app.visibilities;
export const selectVisibility = (key: string) => (state: RootState) => state.app.visibilities[key] ?? true;
export const selectError = (key: string) => (state: RootState) => state.app.errors[key];
export const selectOverride = (key: string) => (state: RootState) => state.app.overrides[key];
export const selectValue = (key: string) => (state: RootState) => state.app.inputs[key] ?? state.app.values[key];

export const selectDerivationTriggerIdQueue = (state: RootState) => state.app.derivationTriggerIdQueue;

export const appReducer = appSlice.reducer;
