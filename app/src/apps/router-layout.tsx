import { Outlet } from "react-router-dom";
import { ScrollRestoration } from "react-router-dom";

export function RouterLayout() {
    return (
        <>
            <ScrollRestoration />
            <Outlet />
        </>
    );
}
