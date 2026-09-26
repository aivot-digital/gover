import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import type {IdentityConfigElement} from '../models/elements/form/input/identity-config-element';
import {InputVariableSource} from '../models/input-mode';
import {ConfirmProvider} from '../providers/confirm-provider';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {
    ViewDispatcherContextProvider,
    ViewDispatcherMode,
} from '../components/view-dispatcher/view-dispatcher.context';
import {IdentityConfigView} from './identity-config-view';

vi.mock('../modules/identity/identity-providers-api-service', () => ({
    IdentityProvidersApiService: class {
        listAll() {
            return Promise.resolve({content: []});
        }
    },
}));

describe('IdentityConfigView', () => {
    it('limits a required identity configuration to one slot', async () => {
        const user = userEvent.setup();
        const element = {
            type: ElementType.IdentityConfigElement,
            id: 'new-identities',
            label: 'Neue Identität',
            required: true,
            maxSlots: 1,
            optionalSlotsAllowed: false,
        } as IdentityConfigElement;

        render(
            <TestViewDispatcherProvider>
                <ConfirmProvider>
                    <IdentityConfigView
                        element={element}
                        value={[{
                            id: 'representative',
                            title: 'Vertretung',
                            description: null,
                            allowsMail: true,
                            isOptional: false,
                            options: [],
                        }]}
                        setValue={vi.fn()}
                        onBlur={vi.fn()}
                        errors={null}
                        isBusy={false}
                        isDeriving={false}
                        authoredElementValues={{}}
                        onAuthoredElementValuesChange={vi.fn()}
                        derivedData={createDerivedRuntimeElementData()}
                        onDerive={async () => createDerivedRuntimeElementData()}
                        onEvent={async () => undefined}
                        onResetErrors={vi.fn()}
                        suppressErrors={false}
                        derivationTriggerIdQueue={[]}
                    />
                </ConfirmProvider>
            </TestViewDispatcherProvider>,
        );

        expect(screen.getByRole('button', {name: 'Hinzufügen'})).toBeDisabled();
        await user.click(screen.getByText('Vertretung').closest('button')!);
        expect(screen.queryByRole('switch', {name: 'Optional'})).not.toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'E-Mail-Adresse zulassen'})).toBeInTheDocument();
    });

    it('exposes the identity list as one field group without including its action in the name', async () => {
        const element = {
            type: ElementType.IdentityConfigElement,
            id: 'identity-config',
            label: 'Benötigte Identitäten',
            hint: 'Legen Sie fest, welche Identitäten benötigt werden.',
            required: true,
            disabled: false,
        } as IdentityConfigElement;

        render(
            <IdentityConfigView
                element={element}
                value={[]}
                setValue={vi.fn()}
                onBlur={vi.fn()}
                errors={null}
                isBusy={false}
                isDeriving={false}
                authoredElementValues={{}}
                onAuthoredElementValuesChange={vi.fn()}
                derivedData={createDerivedRuntimeElementData()}
                onDerive={async () => createDerivedRuntimeElementData()}
                onEvent={async () => undefined}
                onResetErrors={vi.fn()}
                suppressErrors={false}
                derivationTriggerIdQueue={[]}
            />,
        );

        const group = screen.getByTitle('Benötigte Identitäten').closest('fieldset');
        const addButton = screen.getByRole('button', {name: 'Hinzufügen'});

        expect(group).toHaveAccessibleName('Benötigte Identitäten');
        expect(group).toHaveAccessibleDescription('Legen Sie fest, welche Identitäten benötigt werden.');
        expect(group).not.toHaveAccessibleName(/Hinzufügen/);
        expect(group).toHaveAttribute('aria-required', 'true');
        expect(addButton).not.toHaveClass('MuiButton-outlined');

        await waitFor(() => expect(addButton).toBeEnabled());
    });

    it('uses the compact two-line field height for configured identities', async () => {
        const element = {
            type: ElementType.IdentityConfigElement,
            id: 'identity-config',
            label: 'Benötigte Identitäten',
            required: false,
            disabled: false,
        } as IdentityConfigElement;

        const {container} = render(
            <ConfirmProvider>
                <IdentityConfigView
                    element={element}
                    value={[
                        {
                            id: 'applicant',
                            title: 'Antragstellende Person',
                            description: null,
                            allowsMail: false,
                            isOptional: false,
                            options: [
                                {identityProviderKey: 'bund-id', additionalScopes: []},
                                {identityProviderKey: 'bayern-id', additionalScopes: []},
                            ],
                        },
                    ]}
                    setValue={vi.fn()}
                    onBlur={vi.fn()}
                    errors={null}
                    isBusy={false}
                    isDeriving={false}
                    authoredElementValues={{}}
                    onAuthoredElementValuesChange={vi.fn()}
                    derivedData={createDerivedRuntimeElementData()}
                    onDerive={async () => createDerivedRuntimeElementData()}
                    onEvent={async () => undefined}
                    onResetErrors={vi.fn()}
                    suppressErrors={false}
                    derivationTriggerIdQueue={[]}
                />
            </ConfirmProvider>,
        );

        const list = container.querySelector('[data-dialog-list]');
        const item = container.querySelector('[data-dialog-list-item]');
        const title = screen.getByTitle('Antragstellende Person');
        const subtitle = await screen.findByTitle('Verpflichtend · 2 Identitätsanbieter');

        expect(getComputedStyle(list!).minHeight).toBe('52px');
        expect(getComputedStyle(list!).borderTopStyle).toBe('solid');
        expect(getComputedStyle(item!).minHeight).toBe('50px');
        expect(list?.tagName).toBe('UL');
        expect(item?.tagName).toBe('LI');
        expect(item?.querySelector('button button')).toBeNull();
        expect(getComputedStyle(subtitle).fontSize).toBe('12px');
        expect(getComputedStyle(subtitle).color).not.toBe(getComputedStyle(title).color);
        expect(getComputedStyle(subtitle.parentElement!).gap).toBe('2px');
    });

    it('keeps configured identities available for viewing when the form is read-only', async () => {
        const element = {
            type: ElementType.IdentityConfigElement,
            id: 'identity-config',
            label: 'Benötigte Identitäten',
            required: false,
            disabled: false,
        } as IdentityConfigElement;

        render(
            <TestViewDispatcherProvider>
                <ConfirmProvider>
                    <IdentityConfigView
                        element={element}
                        value={[{
                            id: 'applicant',
                            title: 'Antragstellende Person',
                            description: null,
                            allowsMail: false,
                            isOptional: false,
                            options: [],
                        }]}
                        setValue={vi.fn()}
                        onBlur={vi.fn()}
                        errors={null}
                        isBusy
                        isDeriving={false}
                        authoredElementValues={{}}
                        onAuthoredElementValuesChange={vi.fn()}
                        derivedData={createDerivedRuntimeElementData()}
                        onDerive={async () => createDerivedRuntimeElementData()}
                        onEvent={async () => undefined}
                        onResetErrors={vi.fn()}
                        suppressErrors={false}
                        derivationTriggerIdQueue={[]}
                    />
                </ConfirmProvider>
            </TestViewDispatcherProvider>,
        );

        const addButton = screen.getByRole('button', {name: 'Hinzufügen'});
        const viewButton = screen.getByRole('button', {name: 'Ansehen'});

        expect(addButton).toBeDisabled();
        await waitFor(() => expect(viewButton).toBeEnabled());

        fireEvent.click(viewButton);

        expect(screen.getByText('Identität ansehen')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Variable referenzieren'})).toBeDisabled();
        expect(screen.getAllByRole('button', {name: 'Schließen'})).toHaveLength(2);
        expect(screen.queryByRole('button', {name: 'Übernehmen'})).not.toBeInTheDocument();
    });

    it('inserts a variable reference into an identity description', async () => {
        const setValue = vi.fn();
        const user = userEvent.setup();
        const element = {
            type: ElementType.IdentityConfigElement,
            id: 'identity-config',
            label: 'Benötigte Identitäten',
            required: false,
            disabled: false,
        } as IdentityConfigElement;

        render(
            <TestViewDispatcherProvider>
                <ConfirmProvider>
                    <IdentityConfigView
                        element={element}
                        value={[{
                            id: 'applicant',
                            title: 'Antragstellende Person',
                            description: null,
                            allowsMail: false,
                            isOptional: false,
                            options: [],
                        }]}
                        setValue={setValue}
                        onBlur={vi.fn()}
                        errors={null}
                        isBusy={false}
                        isDeriving={false}
                        authoredElementValues={{}}
                        onAuthoredElementValuesChange={vi.fn()}
                        derivedData={createDerivedRuntimeElementData()}
                        onDerive={async () => createDerivedRuntimeElementData()}
                        onEvent={async () => undefined}
                        onResetErrors={vi.fn()}
                        suppressErrors={false}
                        derivationTriggerIdQueue={[]}
                    />
                </ConfirmProvider>
            </TestViewDispatcherProvider>,
        );

        const editButton = screen.getByText('Antragstellende Person').closest('button')!;
        await waitFor(() => expect(editButton).toBeEnabled());
        await user.click(editButton);
        await user.click(await screen.findByRole('button', {name: 'Variable referenzieren'}));
        await user.click(await screen.findByText('Name der Person'));
        await user.click(screen.getByTestId('use-variable-reference'));
        await waitFor(() => {
            expect(screen.queryByRole('heading', {name: 'Variable referenzieren'})).not.toBeInTheDocument();
        });
        await user.click(screen.getByRole('button', {name: 'Übernehmen'}));

        expect(setValue).toHaveBeenCalledWith([
            expect.objectContaining({
                id: 'applicant',
                description: expect.stringContaining('{{ $.person.name }}'),
            }),
        ]);
    });
});

function TestViewDispatcherProvider(props: {children: ReactNode}) {
    const rootElement = generateElementWithDefaultValues(ElementType.FormLayout);

    return <ViewDispatcherContextProvider value={{
        mode: ViewDispatcherMode.Editor,
        rootElement,
        allElements: [rootElement],
        rootAuthoredElementValues: {},
        rootDerivedData: createDerivedRuntimeElementData(),
        inputModeVariables: [{
            source: InputVariableSource.ProcessData,
            path: 'person.name',
            label: 'Name der Person',
        }],
    }}>
        {props.children}
    </ViewDispatcherContextProvider>;
}
