import {Typography} from '@mui/material';
import {useCallback, useMemo, useState} from 'react';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {Permission} from '../../../data/permissions/permission';
import {useApi} from '../../../hooks/use-api';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {SystemConfigsApiService} from '../../configs/system-configs-api-service';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';
import {useConfirm} from '../../../providers/confirm-provider';
import {setSystemConfig} from '../../../slices/system-config-slice';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import type {Theme} from '../models/theme';
import {useThemeReloadPrompt} from './use-theme-reload-prompt';

export function useSetDefaultTheme() {
    const api = useApi();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const promptThemeReload = useThemeReloadPrompt();
    const canSetDefaultTheme = useHasSystemPermission(Permission.SYSTEM_CONFIG_UPDATE);
    const [settingDefaultThemeId, setSettingDefaultThemeId] = useState<number | null>(null);
    const systemConfigsApiService = useMemo(() => new SystemConfigsApiService(api), [api]);

    const setDefaultTheme = useCallback(async (theme: Theme): Promise<boolean> => {
        if (!canSetDefaultTheme || settingDefaultThemeId != null) {
            return false;
        }

        const confirmed = await confirm({
            title: 'Standard-Erscheinungsbild ändern',
            confirmButtonText: 'Als Standard festlegen',
            children: (
                <>
                    <Typography>
                        Möchten Sie <strong>{theme.name}</strong> als Standard-Erscheinungsbild der Prosuna-Instanz
                        festlegen?
                    </Typography>
                    <Typography sx={{mt: 1}}>
                        Es wird überall dort verwendet, wo kein spezifischeres Erscheinungsbild einer
                        Organisationseinheit greift.
                    </Typography>
                </>
            ),
        });

        if (!confirmed) {
            return false;
        }

        setSettingDefaultThemeId(theme.id);

        try {
            const updatedConfig = await systemConfigsApiService.update(SystemConfigKeys.system.theme, {
                value: theme.id.toString(),
            });

            dispatch(setSystemConfig(updatedConfig));
            dispatch(showSuccessSnackbar(`${theme.name} wurde als Standard-Erscheinungsbild festgelegt.`));

            await promptThemeReload();

            return true;
        } catch (error) {
            console.error(error);
            dispatch(showApiErrorSnackbar(error, 'Das Standard-Erscheinungsbild konnte nicht geändert werden.'));
            return false;
        } finally {
            setSettingDefaultThemeId(null);
        }
    }, [
        canSetDefaultTheme,
        confirm,
        dispatch,
        promptThemeReload,
        settingDefaultThemeId,
        systemConfigsApiService,
    ]);

    return {
        canSetDefaultTheme,
        setDefaultThemeDisabledTooltip: canSetDefaultTheme
            ? undefined
            : formatMissingPermissionTooltip(Permission.SYSTEM_CONFIG_UPDATE),
        isSettingDefaultTheme: settingDefaultThemeId != null,
        setDefaultTheme,
    };
}
