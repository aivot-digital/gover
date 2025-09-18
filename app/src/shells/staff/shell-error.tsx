import {useRouteError} from 'react-router-dom';
import {useEffect, useMemo} from 'react';

export function StaffShellError() {
    const error = useRouteError();

    useEffect(() => {

    }, [error]);

    return (
        <div>
            <h1>Something went wrong.</h1>
            <p>Please try again later or contact support if the problem persists.</p>
        </div>
    );
}