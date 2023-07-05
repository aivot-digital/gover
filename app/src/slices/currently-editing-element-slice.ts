import {createSlice, current, PayloadAction} from '@reduxjs/toolkit'
import {RootState} from '../store';
import {AnyElement} from '../models/elements/any-element';

const initialState: {
    edited?: AnyElement;
    original?: AnyElement;
} = {};

// TODO: Check is rquired and remove

const currentlyEditingElementSlice = createSlice({
    name: 'currentlyEditingElement',
    initialState,
    reducers: {
        clearCurrentlyEditingElement: (state, _: PayloadAction) => {
            state.edited = undefined;
            state.original = undefined;
        },
        setCurrentlyEditingElement: (state, action: PayloadAction<AnyElement>) => {
            state.edited = {...action.payload}
            state.original = action.payload;
        },
        updateCurrentlyEditingElement: (state, action: PayloadAction<Partial<AnyElement>>) => {
            const currentModel = current(state.edited);
            if (currentModel == null) {
                return;
            }
            // @ts-ignore TODO
            state.edited = {
                ...currentModel,
                ...action.payload,
            };
        },
    },
});

export const {
    clearCurrentlyEditingElement,
    setCurrentlyEditingElement,
    updateCurrentlyEditingElement,
} = currentlyEditingElementSlice.actions;

export const selectCurrentlyEditingElement = (state: RootState) => state.currentlyEditingElement.edited;
export const selectCurrentlyEditingOriginal = (state: RootState) => state.currentlyEditingElement.original;

export const currentlyEditingElementReducer = currentlyEditingElementSlice.reducer;
