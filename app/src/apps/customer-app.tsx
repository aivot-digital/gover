import React, { useEffect } from 'react';
import { HashRouter, Route, Routes } from 'react-router-dom';
import { ApplicationPage } from '../pages/customer-pages/application-page';
import { fetchPublicSystemConfig } from '../slices/system-config-slice';
import { useDispatch } from 'react-redux';
import { ListPage } from '../pages/customer-pages/list-page';

function CustomerApp(): JSX.Element {
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(fetchPublicSystemConfig());
    }, [dispatch]);

    return (
        <HashRouter>
            <Routes>
                <Route
                    path="/:slug/:version"
                    element={ <ApplicationPage/> }
                />

                <Route
                    path=""
                    element={ <ListPage/> }
                />
            </Routes>
        </HashRouter>
    );
}

export default CustomerApp;
