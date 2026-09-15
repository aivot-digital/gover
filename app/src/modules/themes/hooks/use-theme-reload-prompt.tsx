import {Typography} from '@mui/material';
import {useCallback} from 'react';
import {useConfirm} from '../../../providers/confirm-provider';

export function useThemeReloadPrompt() {
    const confirm = useConfirm();

    return useCallback(async (): Promise<void> => {
        const reloadNow = await confirm({
            title: 'Standard-Erscheinungsbild geändert',
            confirmButtonText: 'Jetzt neu laden',
            cancelButtonText: 'Später',
            children: (
                <Typography>
                    Laden Sie Prosuna neu, um das neue Standard-Erscheinungsbild in der Benutzeroberfläche
                    anzuwenden.
                </Typography>
            ),
        });

        if (reloadNow) {
            window.location.reload();
        }
    }, [confirm]);
}
