import {createSlice, type PayloadAction} from '@reduxjs/toolkit';
import {type RootState} from '../store';
import {type CustomerInput} from '../models/customer-input';

const initialState: {
    input: CustomerInput;
} = {
    input: {},
};

const customerInputSlice = createSlice({
    name: 'customerInput',
    initialState,
    reducers: {
        setUserInput: (state, action: PayloadAction<CustomerInput>) => {
            state.input = action.payload;
        },
        resetUserInput: (state, _: PayloadAction<void>) => {
            state.input = {};
        },
        updateUserInput: (state, action: PayloadAction<{key: string, value: any}>) => {
            state.input[action.payload.key] = action.payload.value;
        },
    },
});

export const {
    updateUserInput,
    setUserInput,
    resetUserInput,
} = customerInputSlice.actions;

export const selectCustomerInput = (state: RootState): CustomerInput => state.customerInput.input;
export const selectCustomerInputValue = (key: string) => (state: RootState) => state.customerInput.input[key];

export const customerInputReducer = customerInputSlice.reducer;
