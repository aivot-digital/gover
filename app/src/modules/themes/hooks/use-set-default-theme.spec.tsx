import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../data/permissions/permission';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {SystemConfigsApiService} from '../../configs/system-configs-api-service';
import {setSystemConfig} from '../../../slices/system-config-slice';
import type {Theme} from '../models/theme';
import {useSetDefaultTheme} from './use-set-default-theme';

const mocks = vi.hoisted(() => ({
    confirm: vi.fn(),
    dispatch: vi.fn(),
    promptThemeReload: vi.fn(),
    useHasSystemPermission: vi.fn(),
}));

vi.mock('../../../hooks/use-api', () => ({
    useApi: () => ({}),
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../providers/confirm-provider', () => ({
    useConfirm: () => mocks.confirm,
}));

vi.mock('../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: mocks.useHasSystemPermission,
}));

vi.mock('./use-theme-reload-prompt', () => ({
    useThemeReloadPrompt: () => mocks.promptThemeReload,
}));

const theme: Theme = {
    id: 42,
    name: 'Nordlicht',
    primaryColor: '#733635',
    secondaryColor: '#A0C9CB',
    primaryColorDark: '#FF613A',
    secondaryColorDark: '#A0C9CB',
    faviconKey: null,
    logoKey: null,
};

describe('useSetDefaultTheme', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        mocks.confirm.mockReset();
        mocks.dispatch.mockReset();
        mocks.promptThemeReload.mockReset();
        mocks.useHasSystemPermission.mockReset();
        mocks.useHasSystemPermission.mockReturnValue(true);
    });

    it('updates the system configuration and offers a reload after confirmation', async () => {
        mocks.confirm.mockResolvedValue(true);
        const updatedConfig = {
            key: SystemConfigKeys.system.theme,
            value: theme.id.toString(),
            publicConfig: true,
        };
        const update = vi.spyOn(SystemConfigsApiService.prototype, 'update')
            .mockResolvedValue(updatedConfig);
        const {result} = renderHook(() => useSetDefaultTheme());

        let changed = false;
        await act(async () => {
            changed = await result.current.setDefaultTheme(theme);
        });

        expect(changed).toBe(true);
        expect(update).toHaveBeenCalledWith(SystemConfigKeys.system.theme, {
            value: theme.id.toString(),
        });
        expect(mocks.dispatch).toHaveBeenCalledWith(setSystemConfig(updatedConfig));
        expect(mocks.dispatch).toHaveBeenCalledTimes(2);
        expect(mocks.promptThemeReload).toHaveBeenCalledOnce();
    });

    it('does not update the configuration without permission', async () => {
        mocks.useHasSystemPermission.mockImplementation((permission: Permission) => (
            permission !== Permission.SYSTEM_CONFIG_UPDATE
        ));
        const update = vi.spyOn(SystemConfigsApiService.prototype, 'update');
        const {result} = renderHook(() => useSetDefaultTheme());

        let changed = true;
        await act(async () => {
            changed = await result.current.setDefaultTheme(theme);
        });

        expect(changed).toBe(false);
        expect(mocks.confirm).not.toHaveBeenCalled();
        expect(update).not.toHaveBeenCalled();
        expect(mocks.promptThemeReload).not.toHaveBeenCalled();
    });
});
