import {configureStore} from '@reduxjs/toolkit'
import {appReducer} from './slices/app-slice';
import {customerInputReducer} from './slices/customer-input-slice';
import {customerInputErrorsReducer} from './slices/customer-input-errors-slice';
import {adminSettingsReducer} from './slices/admin-settings-slice';
import {currentlyEditingElementReducer} from './slices/currently-editing-element-slice';
import {userReducer} from './slices/user-slice';
import {systemConfigReducer} from './slices/system-config-slice';
import {stepperReducer} from './slices/stepper-slice';
import {snackbarReducer} from './slices/snackbar-slice';
import {authReducer} from './slices/auth-slice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        adminSettings: adminSettingsReducer,
        app: appReducer,
        currentlyEditingElement: currentlyEditingElementReducer,
        customerInputErrors: customerInputErrorsReducer,
        customerInput: customerInputReducer,
        user: userReducer,
        systemConfig: systemConfigReducer,
        stepper: stepperReducer,
        snackbar: snackbarReducer,
    },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
