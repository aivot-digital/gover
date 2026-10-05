import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {shellReducer} from '../../../slices/shell-slice';
import {SystemStatusPage} from './system-status-page';

vi.mock('../../../pages/staff-pages/settings/components/system-information/system-information', () => ({
    SystemInformation: () => <div>System information</div>,
}));

describe('SystemStatusPage', () => {
    it('opens the global About Prosuna dialog from the page header', async () => {
        const store = configureStore({
            reducer: {
                shell: shellReducer,
            },
        });
        const user = userEvent.setup();

        render(
            <Provider store={store}>
                <SystemStatusPage/>
            </Provider>,
        );

        await user.click(screen.getByRole('button', {name: 'Über Prosuna v5.x (DEV)'}));

        expect(store.getState().shell.showAboutProsunaDialog).toBe(true);
    });
});
