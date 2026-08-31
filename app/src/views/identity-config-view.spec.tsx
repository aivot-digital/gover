import {render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../data/element-type/element-type';
import {createDerivedRuntimeElementData} from '../models/element-data';
import type {IdentityConfigElement} from '../models/elements/form/input/identity-config-element';
import {ConfirmProvider} from '../providers/confirm-provider';
import {IdentityConfigView} from './identity-config-view';

vi.mock('../modules/identity/identity-providers-api-service', () => ({
    IdentityProvidersApiService: class {
        listAll() {
            return Promise.resolve({content: []});
        }
    },
}));

describe('IdentityConfigView', () => {
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
        const subtitle = await screen.findByTitle('Verpflichtend · 2 Nutzerkontenanbieter');

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
});
