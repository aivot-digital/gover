import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {AnyElement} from '../models/elements/any-element';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../utils/flatten-elements';
import {FormDetailsResponseDTO} from '../modules/forms/dtos/form-details-response-dto';


const initialState: {
    // Future states of the loaded form. These get created when changes are undone
    futureLoadedForm: Array<FormDetailsResponseDTO>,
    // The form that has been loaded
    loadedForm?: FormDetailsResponseDTO;
    // Past states of the loaded form. These get created when changes are done
    pastLoadedForm: Array<FormDetailsResponseDTO>,

    // A list of all elements in the form
    allElements?: AnyElement[];
    // A list of all elements in the form with their parents
    allElementsWithParents?: ElementWithParents[];

    // ID of the dialog to show
    showDialog?: string;
} = {
    futureLoadedForm: [],
    pastLoadedForm: [],
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
        },

        updateLoadedForm: (state, action: PayloadAction<FormDetailsResponseDTO>) => {
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
    },
});

export const {
    clearLoadedForm,
    updateLoadedForm,
    undoLoadedForm,
    redoLoadedForm,
    clearLoadedFormHistory,
    showDialog,
} = appSlice.actions;

export const selectLoadedForm = (state: RootState) => state.app.loadedForm;
export const selectPastLoadedForm = (state: RootState) => state.app.pastLoadedForm;
export const selectFutureLoadedForm = (state: RootState) => state.app.futureLoadedForm;
export const selectAllElements = (state: RootState) => state.app.allElements;

export const appReducer = appSlice.reducer;
