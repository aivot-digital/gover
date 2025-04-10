import {configureStore} from '@reduxjs/toolkit';
import {appReducer} from './slices/app-slice';
import {adminSettingsReducer} from './slices/admin-settings-slice';
import {userReducer} from './slices/user-slice';
import {systemConfigReducer} from './slices/system-config-slice';
import {stepperReducer} from './slices/stepper-slice';
import {snackbarReducer} from './slices/snackbar-slice';
import {authReducer} from './slices/auth-slice';
import {loadingOverlayReducer} from './slices/loading-overlay-slice';
import {loggingReducer} from './slices/logging-slice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        adminSettings: adminSettingsReducer,
        app: appReducer,
        user: userReducer,
        systemConfig: systemConfigReducer,
        stepper: stepperReducer,
        snackbar: snackbarReducer,
        loadingOverlay: loadingOverlayReducer,
        logging: loggingReducer,
    },
    devTools: process.env.NODE_ENV !== 'production',
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
