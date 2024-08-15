import React, {useEffect} from 'react';
import {createBrowserRouter as createRouter, RouterProvider} from 'react-router-dom';
import {setSystemConfigs} from '../slices/system-config-slice';
import {customerAppRoutes} from "./customer-app-routes";
import {ApiService} from "../services/api-service";
import {useAppDispatch} from "../hooks/use-app-dispatch";
import {SystemConfig} from "../models/entities/system-config";

const router = createRouter(
    Object
        .keys(customerAppRoutes)
        .map((key) => customerAppRoutes[key]),
);

function CustomerApp(): JSX.Element {
    const dispatch = useAppDispatch();

    useEffect(() => {
        new ApiService()
            .get<SystemConfig[]>('/api/public/system-configs')
            .then((configs) => {
                dispatch(setSystemConfigs(configs));
            })
            .catch((error) => {
                console.error(error);
            });
    }, []);

    return <RouterProvider router={router}/>;
}

export default CustomerApp;
