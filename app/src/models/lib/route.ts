import {RouteObject} from 'react-router-dom';

export interface Route {
    path: string;
    element: React.ReactNode;
    children?: RouteObject[];
}
