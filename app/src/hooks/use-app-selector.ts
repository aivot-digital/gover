import {type RootState} from '../store';
import {type TypedUseSelectorHook, useSelector} from 'react-redux';

export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
