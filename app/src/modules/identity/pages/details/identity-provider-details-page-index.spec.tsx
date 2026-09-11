import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {
    GenericDetailsPageProvider,
    type GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {type IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {formSchema, IdentityProviderDetailsPageIndex} from './identity-provider-details-page-index';

const testState = vi.hoisted(() => ({
    canReadSecrets: true,
    isBusy: false,
    isEditable: true,
    provider: undefined as IdentityProviderDetailsDTO | undefined,
    handleFieldChange: vi.fn(),
}));

vi.mock('../../../../hooks/use-form-manager', () => ({
    useFormManager: () => ({
        currentItem: testState.provider,
        errors: {},
        hasNotChanged: true,
        handleInputPatch: vi.fn(),
        handleInputBlur: () => vi.fn(),
        handleInputChange: (field: string) => (value: unknown) => testState.handleFieldChange(field, value),
        validate: vi.fn(() => true),
        reset: vi.fn(),
    }),
}));

vi.mock('../../../../hooks/use-change-blocker', () => ({
    useChangeBlocker: () => ({dialog: null}),
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(async () => true),
}));

vi.mock('../../../permissions/hooks/use-permissions', () => ({
    useHasSystemPermission: (permission: string) => permission === 'secret.read'
        ? testState.canReadSecrets
        : true,
}));

vi.mock('../../../secrets/components/secret-select-component', () => ({
    SecretSelectComponent: (props: {
        disabled?: boolean;
        hint?: string;
        onChange: (value: string | null) => void;
        placeholder?: string;
        value?: string | null;
    }) => {
        return (
            <div
                data-testid="secret-select"
                data-disabled={String(Boolean(props.disabled))}
                data-hint={props.hint}
                data-placeholder={props.placeholder}
                data-value={props.value}
            >
                <button type="button" onClick={() => props.onChange('replacement-secret')}>Geheimnis auswählen</button>
                <button type="button" onClick={() => props.onChange(null)}>Geheimnis entfernen</button>
            </div>
        );
    },
}));

vi.mock('../../../assets/components/image-selector', () => ({
    ImageSelector: () => null,
}));

vi.mock('../../components/identity-provider-icon/identity-provider-icon', () => ({
    IdentityProviderIcon: () => null,
}));

vi.mock('../../../../components/string-list-input/string-list-input', () => ({
    StringListInput: (props: {
        busy?: boolean;
        disabled?: boolean;
        label: string;
        readOnly?: boolean;
    }) => (
        <div
            data-testid={props.label}
            data-busy={String(Boolean(props.busy))}
            data-disabled={String(Boolean(props.disabled))}
            data-readonly={String(Boolean(props.readOnly))}
        />
    ),
}));

vi.mock('../../../../components/table-field/table-field-component-2', () => ({
    TableFieldComponent2: (props: {
        busy?: boolean;
        disabled?: boolean;
        label: string;
        readOnly?: boolean;
    }) => (
        <div
            data-testid={props.label}
            data-busy={String(Boolean(props.busy))}
            data-disabled={String(Boolean(props.disabled))}
            data-readonly={String(Boolean(props.readOnly))}
        />
    ),
}));

vi.mock('../../../../components/select-field/select-field-component', () => ({
    SelectFieldComponent: (props: {
        disabled?: boolean;
        onChange: (value: string | null) => void;
        options: Array<{label: string; subLabel?: string; value: string}>;
        value?: string | null;
    }) => (
        <div
            data-testid="unique-id-attribute-select"
            data-disabled={String(Boolean(props.disabled))}
            data-options={JSON.stringify(props.options)}
            data-value={props.value}
        >
            <button type="button" onClick={() => props.onChange('preferred_username')}>Attribut auswählen</button>
        </div>
    ),
}));

vi.mock('../../../../dialogs/confirm-dialog/confirm-dialog', () => ({
    ConfirmDialog: () => null,
}));

vi.mock('../../../../dialogs/constraint-dialog/constraint-dialog', () => ({
    ConstraintDialog: () => null,
}));

describe('IdentityProviderDetailsPageIndex', () => {
    beforeEach(() => {
        testState.canReadSecrets = true;
        testState.isBusy = false;
        testState.isEditable = true;
        testState.provider = createProvider();
        testState.handleFieldChange.mockReset();
    });

    it('uses the dedicated secret selector and stores only its selected key', () => {
        renderPage();

        expect(screen.getByTestId('secret-select')).toHaveAttribute('data-value', 'existing-secret');
        expect(screen.getByTestId('secret-select')).toHaveAttribute('data-disabled', 'false');

        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis auswählen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Geheimnis entfernen'}));

        expect(testState.handleFieldChange).toHaveBeenNthCalledWith(1, 'clientSecretKey', 'replacement-secret');
        expect(testState.handleFieldChange).toHaveBeenNthCalledWith(2, 'clientSecretKey', undefined);
        expect(screen.queryByRole('button', {name: /Geheimnisse neu laden/})).not.toBeInTheDocument();
    });

    it('keeps the secret selector disabled without secret read permission', () => {
        testState.canReadSecrets = false;

        renderPage();

        expect(screen.getByTestId('secret-select')).toHaveAttribute('data-disabled', 'true');
        expect(screen.getByTestId('secret-select')).toHaveAttribute(
            'data-hint',
            'Sie besitzen nicht die erforderliche Berechtigung (secret.read).',
        );
        expect(screen.getByTestId('secret-select')).not.toHaveAttribute('data-value');
        expect(screen.getByTestId('secret-select')).toHaveAttribute(
            'data-placeholder',
            'Keine Berechtigung zur Einsicht',
        );
    });

    it('keeps the secret selector disabled for system providers', () => {
        testState.provider = createProvider(IdentityProviderType.BundID);

        renderPage();

        expect(screen.getByTestId('secret-select')).toHaveAttribute('data-disabled', 'true');
        expect(screen.getByTestId('unique-id-attribute-select')).toHaveAttribute('data-disabled', 'true');
    });

    it('keeps all system provider tables navigable and read-only', () => {
        testState.provider = createProvider(IdentityProviderType.BundID);

        renderPage();

        for (const label of ['Scopes', 'Zusätzliche Parameter', 'Attributszuweisungen']) {
            const table = screen.getByTestId(label);
            expect(table).toHaveAttribute('data-disabled', 'false');
            expect(table).toHaveAttribute('data-readonly', 'true');
            expect(table).toHaveAttribute('data-busy', 'false');
        }
    });

    it('uses read-only tables when update permission is missing', () => {
        testState.isEditable = false;

        renderPage();

        for (const label of ['Scopes', 'Zusätzliche Parameter', 'Attributszuweisungen']) {
            const table = screen.getByTestId(label);
            expect(table).toHaveAttribute('data-disabled', 'false');
            expect(table).toHaveAttribute('data-readonly', 'true');
        }
    });

    it('keeps custom provider tables editable', () => {
        renderPage();

        for (const label of ['Scopes', 'Zusätzliche Parameter', 'Attributszuweisungen']) {
            const table = screen.getByTestId(label);
            expect(table).toHaveAttribute('data-disabled', 'false');
            expect(table).toHaveAttribute('data-readonly', 'false');
        }
    });

    it('marks configuration tables busy during requests', () => {
        testState.isBusy = true;

        renderPage();

        for (const label of ['Scopes', 'Zusätzliche Parameter', 'Attributszuweisungen']) {
            const table = screen.getByTestId(label);
            expect(table).toHaveAttribute('data-disabled', 'false');
            expect(table).toHaveAttribute('data-busy', 'true');
        }
    });

    it('selects the unique ID from the configured attribute mappings', () => {
        testState.provider = {
            ...createProvider(),
            attributes: [
                {
                    label: 'Subject Identifier',
                    description: 'Unique provider identity',
                    keyInData: 'sub',
                    displayAttribute: false,
                },
                {
                    label: 'Benutzername',
                    description: 'Unique account name',
                    keyInData: 'preferred_username',
                    displayAttribute: false,
                },
            ],
        };

        renderPage();

        const select = screen.getByTestId('unique-id-attribute-select');
        expect(select).toHaveAttribute('data-value', 'sub');
        expect(JSON.parse(select.getAttribute('data-options') ?? '[]')).toEqual([
            {label: 'Subject Identifier', subLabel: 'sub', value: 'sub'},
            {label: 'Benutzername', subLabel: 'preferred_username', value: 'preferred_username'},
        ]);

        fireEvent.click(screen.getByRole('button', {name: 'Attribut auswählen'}));
        expect(testState.handleFieldChange).toHaveBeenCalledWith('uniqueIdAttribute', 'preferred_username');
    });

    it('validates that the unique ID references an attribute mapping', async () => {
        const provider = {...createProvider(), iconAssetKey: 'asset-key'};

        await expect(formSchema.validate(provider)).resolves.toBeDefined();
        await expect(formSchema.validate({...provider, uniqueIdAttribute: ''})).rejects.toThrow(
            'Das Attribut für die eindeutige ID ist ein Pflichtfeld.',
        );
        await expect(formSchema.validate({...provider, uniqueIdAttribute: 'email'})).rejects.toThrow(
            'Das Attribut für die eindeutige ID muss in den Attributszuweisungen enthalten sein.',
        );
    });
});

function renderPage() {
    const context: GenericDetailsPageContextType<IdentityProviderDetailsDTO, void> = {
        item: testState.provider,
        setItem: vi.fn(),
        isNewItem: false,
        isExistingItem: true,
        setAdditionalData: vi.fn(),
        isBusy: testState.isBusy,
        setIsBusy: vi.fn(),
        refresh: vi.fn(),
        isEditable: testState.isEditable,
    };

    return render(
        <MemoryRouter>
            <GenericDetailsPageProvider value={context}>
                <IdentityProviderDetailsPageIndex/>
            </GenericDetailsPageProvider>
        </MemoryRouter>,
    );
}

function createProvider(type: IdentityProviderType = IdentityProviderType.Custom): IdentityProviderDetailsDTO {
    return {
        key: 'provider-key',
        metadataIdentifier: 'provider',
        uniqueIdAttribute: 'sub',
        type,
        name: 'Test Provider',
        description: 'Identity provider used in this test.',
        iconAssetKey: null,
        attributes: [{
            label: 'Subject Identifier',
            description: 'Unique provider identity',
            keyInData: 'sub',
            displayAttribute: false,
        }],
        isEnabled: false,
        isTestProvider: false,
        authorizationEndpoint: 'https://example.com/authorize',
        tokenEndpoint: 'https://example.com/token',
        userinfoEndpoint: null,
        endSessionEndpoint: null,
        clientId: 'test-client',
        clientSecretKey: 'existing-secret',
        defaultScopes: [],
        additionalParams: [],
        pkceMethod: null,
    };
}
