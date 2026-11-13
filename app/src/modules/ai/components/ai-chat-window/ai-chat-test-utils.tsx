import {configureStore} from '@reduxjs/toolkit';
import {render} from '@testing-library/react';
import type {ReactElement} from 'react';
import {Provider} from 'react-redux';
import {userReducer} from '../../../../slices/user-slice';

export function renderAiChatTestUi(ui: ReactElement) {
    const store = configureStore({reducer: {user: userReducer}});
    return render(ui, {
        wrapper: ({children}) => <Provider store={store}>{children}</Provider>,
    });
}
