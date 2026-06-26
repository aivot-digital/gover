import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store.staff';
import {AnyElement} from '../models/elements/any-element';
import {ElementWithParents, flattenElements, flattenElementsWithParents} from '../utils/flatten-elements';
import {FormEntity} from '../modules/forms/entities/form-entity';
import {FormVersionEntity} from '../modules/forms/entities/form-version-entity';
import {VFormWithPermissionsEntity} from '../modules/forms/entities/v-form-with-permissions-entity';

const initialState: {
    // ID of the dialog to show
    showDialog?: string;
} = {
};

const appSlice = createSlice({
    name: 'app',
    initialState,
    reducers: {
        showDialog: (state, action: PayloadAction<string | undefined>) => {
            state.showDialog = action.payload;
        },
    },
});

export const {
    showDialog,
} = appSlice.actions;

export const appReducer = appSlice.reducer;
