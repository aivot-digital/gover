import React, {useEffect} from 'react';
import {HashRouter, Route, Routes} from 'react-router-dom';
import {Application} from '../pages/customer-pages/application';
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
                <Route path="/:slug/:version" element={<Application/>}/>
            </Routes>
        </HashRouter>
    );
}

export default CustomerApp;
