import {RouteObject} from 'react-router-dom';

export interface Route {
    path: string;
    element: JSX.Element;
    children?: RouteObject[];
}
