import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useEffect } from 'react';
import { selectUser } from '../slices/user-slice';
import { isAnonymousUser, isInvalidUser } from '../models/entities/user';

export function useAuthGuard(): void {
    const navigate = useNavigate();
    const user = useSelector(selectUser);

    useEffect(() => {
        if (user == null || isAnonymousUser(user) || isInvalidUser(user)) {
            navigate('/');
        }
    }, [navigate, user]);
}
