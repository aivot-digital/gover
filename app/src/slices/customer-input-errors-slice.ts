import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import {CustomerInputErrors} from '../models/customer-input-errors';
import {RootState} from '../store';

const initialState: {
    errors: CustomerInputErrors;
} = {
    errors: {},
};

const customerInputErrorsSlice = createSlice({
    name: 'customerInputErrors',
    initialState,
    reducers: {
        resetErrors: (state, _: PayloadAction<void>) => {
            state.errors = {};
        },
        addError: (state, action: PayloadAction<{ key: string, error: string; }>) => {
            // console.log(`Adding error for ${action.payload.key}`, action.payload.error);
            state.errors[action.payload.key] = action.payload.error;
        },
    },
});

export const {
    resetErrors,
    addError,
} = customerInputErrorsSlice.actions;

export const selectCustomerInputErrors = (state: RootState) => state.customerInputErrors.errors;
export const selectCustomerInputErrorValue = (key: string) => (state: RootState) => state.customerInputErrors.errors[key];

export const customerInputErrorsReducer = customerInputErrorsSlice.reducer;
