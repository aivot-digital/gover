import {type RootState} from '../store.staff';
import {type TypedUseSelectorHook, useSelector} from 'react-redux';

export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
