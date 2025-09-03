import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {type Form as Application, FormPublicProjection} from '../models/entities/form';
import {AnyElement} from '../models/elements/any-element';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../utils/flatten-elements';


const initialState: {
    // Future states of the loaded form. These get created when changes are undone
    futureLoadedForm: Array<Application | FormPublicProjection>,
    // The form that has been loaded
    loadedForm?: Application | FormPublicProjection;
    // Past states of the loaded form. These get created when changes are done
    pastLoadedForm: Array<Application | FormPublicProjection>,

    // A list of all elements in the form
    allElements?: AnyElement[];
    // A list of all elements in the form with their parents
    allElementsWithParents?: ElementWithParents[];

    // ID of the dialog to show
    showDialog?: string;

    // Whether the customer input has been loaded from the local storage
    hasLoadedSavedCustomerInput: boolean;
} = {
    futureLoadedForm: [],
    pastLoadedForm: [],

    hasLoadedSavedCustomerInput: false,
};

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        clearLoadedForm: (state, _: PayloadAction<void>) => {
            state.loadedForm = undefined;
            state.allElements = undefined;
            state.allElementsWithParents = undefined;
            state.futureLoadedForm = [];
            state.pastLoadedForm = [];
            state.showDialog = undefined;
            state.hasLoadedSavedCustomerInput = false;
        },

        updateLoadedForm: (state, action: PayloadAction<Application | FormPublicProjection>) => {
            if (state.loadedForm != null) {
                state.pastLoadedForm.push(state.loadedForm);
                state.futureLoadedForm = [];
            }
            state.loadedForm = action.payload;
            state.allElements = flattenElements(state.loadedForm.rootElement);
            state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.rootElement, [], false);
        },

        undoLoadedForm: (state, _: PayloadAction<void>) => {
            if (state.pastLoadedForm.length > 0 && state.loadedForm != null) {
                state.futureLoadedForm.push(state.loadedForm);
                state.loadedForm = state.pastLoadedForm.pop();
                if (state.loadedForm != null) {
                    state.allElements = flattenElements(state.loadedForm.rootElement);
                    state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.rootElement, [], false);
                } else {
                    state.allElements = undefined;
                    state.allElementsWithParents = undefined;
                }
            }
        },

        redoLoadedForm: (state, _: PayloadAction<void>) => {
            if (state.futureLoadedForm.length > 0 && state.loadedForm != null) {
                state.pastLoadedForm.push(state.loadedForm);
                state.loadedForm = state.futureLoadedForm.pop();
                if (state.loadedForm != null) {
                    state.allElements = flattenElements(state.loadedForm.rootElement);
                    state.allElementsWithParents = flattenElementsWithParents(state.loadedForm.rootElement, [], false);
                } else {
                    state.allElements = undefined;
                    state.allElementsWithParents = undefined;
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

        flagLoadedSavedCustomerInput: (state, action: PayloadAction<void>) => {
            state.hasLoadedSavedCustomerInput = true;
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
    flagLoadedSavedCustomerInput,
} = appSlice.actions;

export const selectLoadedForm = (state: RootState) => state.app.loadedForm;
export const selectPastLoadedForm = (state: RootState) => state.app.pastLoadedForm;
export const selectFutureLoadedForm = (state: RootState) => state.app.futureLoadedForm;
export const selectHasLoadedSavedCustomerInput = (state: RootState) => state.app.hasLoadedSavedCustomerInput;
export const selectAllElements = (state: RootState) => state.app.allElements;

export const appReducer = appSlice.reducer;
