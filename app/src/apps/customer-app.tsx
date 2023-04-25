import React, {useEffect} from 'react';
import {HashRouter, Route, Routes} from 'react-router-dom';
import {ApplicationPage} from '../pages/customer-pages/application-page';
import {fetchSystemConfig} from '../slices/system-config-slice';
import {useDispatch} from 'react-redux';


function CustomerApp() {
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(fetchSystemConfig());
    }, [dispatch]);

    return (
        <HashRouter>
            <Routes>
                <Route
                    path="/:slug/:version"
                    element={<ApplicationPage/>}
                />
            </Routes>
        </HashRouter>
    );
}

export default CustomerApp;
