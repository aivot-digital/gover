import {configureStore} from '@reduxjs/toolkit';
import {appReducer} from './slices/app-slice';
import {systemConfigReducer} from './slices/system-config-slice';
import {stepperReducer} from './slices/stepper-slice';
import {identityReducer} from './slices/identity-slice';
import {shellReducer} from './slices/shell-slice';
import {authReducer} from './slices/auth-slice';
import {adminSettingsReducer} from './slices/admin-settings-slice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        adminSettings: adminSettingsReducer,
        identity: identityReducer,
        app: appReducer,
        systemConfig: systemConfigReducer,
        stepper: stepperReducer,
        shell: shellReducer,
    },
});
