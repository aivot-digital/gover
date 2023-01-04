import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {useEffect} from 'react';
import {selectAuthenticationState} from '../slices/auth-slice';

export function useAuthGuard(): void {
    const navigate = useNavigate();
    const authState = useSelector(selectAuthenticationState);

    useEffect(() => {
        if (authState !== 'is-authenticated') {
            navigate('/');
        }
    }, [navigate, authState]);
}
