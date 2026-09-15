import {LinearProgress} from '@mui/material';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectLoadingMessage} from '../../../slices/shell-slice';

export function ShellProgress() {
    const loadingMessage = useAppSelector(selectLoadingMessage);

    if (!loadingMessage || loadingMessage.blocking) {
        return null;
    }

    return (
        <LinearProgress
            sx={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                zIndex: (theme) => theme.zIndex.drawer + 1,
            }}
        />
    );
}