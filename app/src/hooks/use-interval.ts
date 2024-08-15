import {DependencyList, useEffect, useMemo, useRef} from 'react';
import {useMembershipsApi} from './use-memberships-api';
import {setMemberships} from '../slices/user-slice';

export function useInterval(callback: () => void, delay: number, dependencies: DependencyList) {
    const interval = useRef<NodeJS.Timer>();
    useEffect(() => {
        if (interval.current) {
            clearInterval(interval.current);
        }
        callback();
        interval.current = setInterval(() => {
            callback();
        }, delay);
        return () => clearInterval(interval.current);
    }, dependencies);
}