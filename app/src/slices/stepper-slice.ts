import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {RootState} from '../store';

const initialState: {
    currentStep: number;
    upcomingStepDirection: 'next' | 'previous' | undefined;
} = {
    currentStep: 0,
    upcomingStepDirection: undefined,
};

export const stepperSlice = createSlice({
    name: 'stepper',
    initialState,
    reducers: {
        nextStep: (state, _: PayloadAction<void>) => {
            state.currentStep = state.currentStep + 1;
            state.upcomingStepDirection = 'next';
        },
        previousStep: (state, _: PayloadAction<void>) => {
            if (state.currentStep > 0) {
                state.currentStep = state.currentStep - 1;
                state.upcomingStepDirection = 'previous';
            }
        },
        setCurrentStep: (state, action: PayloadAction<number>) => {
            if (action.payload >= 0) {
                state.currentStep = action.payload;
                state.upcomingStepDirection = 'previous';
            }
        },
        resetStepper: (state, _: PayloadAction<void>) => {
            state.currentStep = 0;
            state.upcomingStepDirection = undefined;
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
export const selectUpcomingStepDirection = (state: RootState) => state.stepper.upcomingStepDirection;

export const stepperReducer = stepperSlice.reducer;
