import {act, render, screen, waitFor} from '@testing-library/react';
import {configureStore} from '@reduxjs/toolkit';
import {Provider} from 'react-redux';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Permission} from '../../../../../data/permissions/permission';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {type Page} from '../../../../../models/dtos/page';
import {SystemConfigsApiService} from '../../../../../modules/configs/system-configs-api-service';
import {type SystemConfigResponseDto} from '../../../../../modules/configs/dtos/system-config-response-dto';
import {SystemRolesApiService} from '../../../../../modules/system/services/system-roles-api-service';
import {type SystemRoleEntity} from '../../../../../modules/system/entities/system-role-entity';
import {ThemesApiService} from '../../../../../modules/themes/themes-api-service';
import {type ThemeResponseDTO} from '../../../../../modules/themes/models/theme';
import {systemConfigReducer} from '../../../../../slices/system-config-slice';
import {ApplicationSettings} from './application-settings';

const mocks = vi.hoisted(() => ({
    api: {},
    confirm: vi.fn(),
    promptThemeReload: vi.fn(),
    useHasSystemPermission: vi.fn(),
}));

vi.mock('../../../../../hooks/use-api', () => ({
    useApi: () => mocks.api,
}));

vi.mock('../../../../../providers/confirm-provider', () => ({
    useConfirm: () => mocks.confirm,
}));

vi.mock('../../../../../modules/themes/hooks/use-theme-reload-prompt', () => ({
    useThemeReloadPrompt: () => mocks.promptThemeReload,
}));

vi.mock('../../../../../modules/permissions/hooks/use-permissions', () => ({
    useHasAnyDepartmentPermission: () => false,
    useHasSystemPermission: mocks.useHasSystemPermission,
    useRequireSystemPermission: vi.fn(),
}));

vi.mock('../../../../../modules/departments/components/department-select-field', () => ({
    DepartmentSelectField: () => null,
}));

vi.mock('../../../../../modules/elements/components/element-derivation-context', () => ({
    ElementDerivationContext: () => null,
}));

const systemRoles: SystemRoleEntity[] = [
    {
        id: 1,
        name: 'Administration',
        description: null,
        permissions: [],
        created: '2026-08-11T08:00:00Z',
        updated: '2026-08-11T08:00:00Z',
    },
    {
        id: 3,
        name: 'Automatischer Import',
        description: null,
        permissions: [],
        created: '2026-08-11T08:00:00Z',
        updated: '2026-08-11T08:00:00Z',
    },
];

const themes: ThemeResponseDTO[] = [
    {
        id: 42,
        name: 'Nordlicht',
        primaryColor: '#005f73',
        secondaryColor: '#ee9b00',
        primaryColorDark: null,
        secondaryColorDark: null,
        faviconKey: null,
        logoKey: null,
    },
];

const systemConfigs: SystemConfigResponseDto[] = [
    {
        key: SystemConfigKeys.users.defaultSystemRole,
        value: '3',
        publicConfig: false,
    },
    {
        key: SystemConfigKeys.systemRoles.mostPrivilegedRole,
        value: '1',
        publicConfig: false,
    },
    {
        key: SystemConfigKeys.system.theme,
        value: '42',
        publicConfig: true,
    },
];

describe('ApplicationSettings', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
        mocks.confirm.mockReset();
        mocks.promptThemeReload.mockReset();
        mocks.useHasSystemPermission.mockReset();
        mocks.useHasSystemPermission.mockImplementation((permission: Permission) => (
            permission === Permission.SYSTEM_CONFIG_UPDATE ||
            permission === Permission.SYSTEM_ROLE_READ ||
            permission === Permission.THEME_READ
        ));

        vi.spyOn(SystemConfigsApiService.prototype, 'listDefinitions').mockResolvedValue([]);
        vi.spyOn(SystemConfigsApiService.prototype, 'listAll').mockResolvedValue(page(systemConfigs));
        vi.spyOn(SystemRolesApiService.prototype, 'listAll').mockResolvedValue(page(systemRoles));
        vi.spyOn(ThemesApiService.prototype, 'listAll').mockResolvedValue(page(themes));
    });

    it('hydrates both role selections when the roles arrive before the configurations', async () => {
        const configsRequest = deferred<Page<SystemConfigResponseDto>>();
        vi.mocked(SystemConfigsApiService.prototype.listAll).mockReturnValue(configsRequest.promise);

        const {store} = renderApplicationSettings();

        await screen.findAllByText('Gespeicherte Systemeinstellungen werden geladen…');
        expect(screen.queryByText('Bitte wählen Sie eine Standard-Systemrolle aus.')).not.toBeInTheDocument();
        expect(screen.queryByText(
            'Bitte wählen Sie die Systemrolle mit der höchsten Berechtigungsstufe aus.',
        )).not.toBeInTheDocument();

        await act(async () => {
            configsRequest.resolve(page(systemConfigs));
        });

        await expectRoleSelections();
        expect(store.getState().systemConfig[SystemConfigKeys.users.defaultSystemRole]).toBe('3');
        expect(store.getState().systemConfig[SystemConfigKeys.systemRoles.mostPrivilegedRole]).toBe('1');
    });

    it('hydrates both role selections when the configurations arrive before the roles', async () => {
        const rolesRequest = deferred<Page<SystemRoleEntity>>();
        vi.mocked(SystemRolesApiService.prototype.listAll).mockReturnValue(rolesRequest.promise);

        renderApplicationSettings();

        await waitFor(() => {
            expect(SystemConfigsApiService.prototype.listAll).toHaveBeenCalledOnce();
        });
        expect(screen.queryByText('Bitte wählen Sie eine Standard-Systemrolle aus.')).not.toBeInTheDocument();

        await act(async () => {
            rolesRequest.resolve(page(systemRoles));
        });

        await expectRoleSelections();
    });

    it('keeps the appearance section in the layout while themes are loading', async () => {
        const themesRequest = deferred<Page<ThemeResponseDTO>>();
        vi.mocked(ThemesApiService.prototype.listAll).mockReturnValue(themesRequest.promise);

        renderApplicationSettings();

        expect(screen.getByText('Standard-Erscheinungsbild der Prosuna-Instanz')).toBeInTheDocument();
        expect(screen.getByText('Prosuna Marktplatz')).toBeInTheDocument();
        expect(screen.getByText('Erscheinungsbilder werden geladen…')).toBeInTheDocument();

        await act(async () => {
            themesRequest.resolve(page(themes));
        });

        await waitFor(() => {
            expect(getSelect('Standard-Erscheinungsbild')).toHaveTextContent('Nordlicht');
        });
    });

    it('shows a loading error without reporting missing role selections', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.mocked(SystemConfigsApiService.prototype.listAll).mockRejectedValue(new Error('Request failed'));

        renderApplicationSettings();

        expect(await screen.findByText(
            /Die gespeicherten Systemeinstellungen konnten nicht geladen werden\. Laden Sie die Seite neu/,
        )).toBeInTheDocument();
        expect(screen.queryByText('Bitte wählen Sie eine Standard-Systemrolle aus.')).not.toBeInTheDocument();
        expect(screen.queryByText(
            'Bitte wählen Sie die Systemrolle mit der höchsten Berechtigungsstufe aus.',
        )).not.toBeInTheDocument();
        expect(getSelect('Standard-Systemrolle für automatische Benutzerimporte')).toHaveAttribute(
            'aria-disabled',
            'true',
        );
    });
});

function renderApplicationSettings() {
    const store = configureStore({
        reducer: {
            systemConfig: systemConfigReducer,
        },
    });

    return {
        store,
        ...render(
            <Provider store={store}>
                <ApplicationSettings/>
            </Provider>,
        ),
    };
}

async function expectRoleSelections(): Promise<void> {
    await waitFor(() => {
        expect(getSelect('Standard-Systemrolle für automatische Benutzerimporte'))
            .toHaveTextContent('Automatischer Import');
        expect(getSelect('Systemrolle mit höchster Berechtigungsstufe'))
            .toHaveTextContent('Administration');
    });
}

function getSelect(name: string): HTMLElement {
    const label = screen.getByTitle(name);
    const controlId = label.getAttribute('for');

    expect(controlId).not.toBeNull();
    const control = document.getElementById(controlId!);
    expect(control).not.toBeNull();

    return control!;
}

function page<T>(content: T[]): Page<T> {
    return {
        content,
        page: {
            size: content.length,
            number: 0,
            totalElements: content.length,
            totalPages: 1,
        },
    };
}

function deferred<T>(): {
    promise: Promise<T>;
    resolve: (value: T) => void;
} {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((res) => {
        resolve = res;
    });

    return {promise, resolve};
}
