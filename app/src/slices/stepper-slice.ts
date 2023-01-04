import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import {RootState} from '../store';

const initialState: {
    currentStep: number;
} = {
    currentStep: 0,
};

export const stepperSlice = createSlice({
    name: 'stepper',
    initialState,
    reducers: {
        nextStep: (state, _: PayloadAction<void>) => {
            state.currentStep = state.currentStep + 1;
        },
        previousStep: (state, _: PayloadAction<void>) => {
            if (state.currentStep > 0) {
                state.currentStep = state.currentStep - 1;
            }
        },
        setCurrentStep: (state, action: PayloadAction<number>) => {
            if (action.payload >= 0) {
                state.currentStep = action.payload;
            }
        },
        resetStepper: (state, _: PayloadAction<void>) => {
            state.currentStep = 0;
        },
    },
});
export const {
    previousStep,
    nextStep,
    setCurrentStep,
    resetStepper,
} = stepperSlice.actions;

export const selectCurrentStep = (state: RootState) => state.stepper.currentStep;

export const stepperReducer = stepperSlice.reducer;
