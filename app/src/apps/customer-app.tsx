import React, {useEffect} from 'react';
import {createBrowserRouter as createRouter, RouterProvider} from 'react-router-dom';
import {setSystemConfigs} from '../slices/system-config-slice';
import {customerAppRoutes} from './customer-app-routes';
import {ApiService} from '../services/api-service';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {Error} from '../pages/shared/error/error';
import {SystemConfigResponseDto} from '../modules/configs/dtos/system-config-response-dto';
import {Page} from '../models/dtos/page';

const router = createRouter(
    [
        {
            errorElement: <Error />,
            children: Object
                .keys(customerAppRoutes)
                .map((key) => customerAppRoutes[key]),
        },
    ],
);

function CustomerApp() {
    const dispatch = useAppDispatch();

    useEffect(() => {
        new ApiService()
            .get<Page<SystemConfigResponseDto>>('/api/public/system-configs/')
            .then((configs) => {
                dispatch(setSystemConfigs(configs.content));
            })
            .catch((error) => {
                console.error(error);
            });
    }, []);

    return <RouterProvider router={router} />;
}

export default CustomerApp;
