import {RouteObject} from 'react-router-dom';

import type { JSX } from "react";

export interface Route {
    path: string;
    element: JSX.Element;
    children?: RouteObject[];
}
