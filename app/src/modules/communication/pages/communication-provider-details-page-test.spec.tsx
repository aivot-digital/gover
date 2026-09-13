import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {type ComputedElementErrors} from '../../../models/element-data';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {type CommunicationProvider, type CommunicationTestingLayout} from '../models';
import {ElementsApiService} from '../../elements/elements-api-service';
import {CommunicationProviderDetailsPageTest} from './communication-provider-details-page-test';

const testState = vi.hoisted(() => ({
    provider: undefined as CommunicationProvider | undefined,
    isNewItem: false,
    dispatch: vi.fn(),
    derivationContextProps: null as Record<string, any> | null,
    changeBlockerProps: null as Record<string, any> | null,
    showApiErrorSnackbar: vi.fn((error: unknown, message: string) => ({type: 'api-error', error, message})),
    showErrorSnackbar: vi.fn((message: string) => ({type: 'error', message})),
    showSuccessSnackbar: vi.fn((message: string) => ({type: 'success', message})),
}));

vi.mock('../../../components/generic-details-page/generic-details-page-context', () => ({
    useGenericDetailsPageContext: () => ({
        item: testState.provider,
        isNewItem: testState.isNewItem,
    }),
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => testState.dispatch,
}));

vi.mock('../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: (props: Record<string, any>) => {
        testState.changeBlockerProps = props;
        return {dialog: null};
    },
}));

vi.mock('../../../slices/snackbar-slice', () => ({
    showApiErrorSnackbar: testState.showApiErrorSnackbar,
    showErrorSnackbar: testState.showErrorSnackbar,
    showSuccessSnackbar: testState.showSuccessSnackbar,
}));

vi.mock('../../elements/components/element-derivation-context', () => ({
    ElementDerivationContext: (props: Record<string, any>) => {
        testState.derivationContextProps = props;
        return (
            <div data-testid="element-derivation-context">
                <button
                    type="button"
                    onClick={() => props.onAuthoredElementValuesChange({
                        'test-recipient': 'test@example.com',
                    })}
                >
                    Testeingaben setzen
                </button>
            </div>
        );
    },
}));

const testingLayout = {
    type: ElementType.GroupLayout,
    id: 'communication-provider-test',
    children: [{
        type: ElementType.Text,
        id: 'test-recipient',
    }],
} as CommunicationTestingLayout;

describe('CommunicationProviderDetailsPageTest', () => {
    beforeEach(() => {
        testState.provider = createProvider();
        testState.isNewItem = false;
        testState.dispatch.mockReset();
        testState.derivationContextProps = null;
        testState.changeBlockerProps = null;
        testState.showApiErrorSnackbar.mockClear();
        testState.showErrorSnackbar.mockClear();
        testState.showSuccessSnackbar.mockClear();
    });

    it('renders the testing layout, validates the inputs and starts the provider test', async () => {
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout')
            .mockResolvedValue(testingLayout);
        const testProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider')
            .mockResolvedValue();
        const derive = vi.spyOn(ElementsApiService.prototype, 'derive')
            .mockResolvedValue({effectiveValues: {}, elementStates: {}});

        render(<CommunicationProviderDetailsPageTest/>);

        await screen.findByTestId('element-derivation-context');
        fireEvent.click(screen.getByRole('button', {name: 'Testeingaben setzen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Kommunikationsanbieter testen'}));

        const expectedInputs = {'test-recipient': 'test@example.com'};
        await waitFor(() => expect(testProvider).toHaveBeenCalledWith(7, expectedInputs));
        expect(derive).toHaveBeenCalledWith({
            element: testingLayout,
            authoredElementValues: expectedInputs,
            derivationOptions: {
                skipErrorsForElementIds: [],
                skipVisibilitiesForElementIds: [],
                skipOverridesForElementIds: [],
                skipValuesForElementIds: [],
            },
            processExecutionData: {
                $: {},
                $$: {},
                _: {},
            },
        });
        expect(testState.showSuccessSnackbar)
            .toHaveBeenCalledWith('Kommunikationsanbieter wurde erfolgreich getestet.');
        expect(testState.changeBlockerProps?.edited).toEqual(expectedInputs);
        expect(testState.changeBlockerProps?.customTitle).toBe('Testeingaben verwerfen?');
    });

    it('shows derived field errors and does not start an invalid test', async () => {
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout')
            .mockResolvedValue(testingLayout);
        const testProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider')
            .mockResolvedValue();
        const elementStates: ComputedElementErrors = {
            'test-recipient': {error: 'Die Testempfängeradresse ist erforderlich.'},
        };
        vi.spyOn(ElementsApiService.prototype, 'derive')
            .mockResolvedValue({effectiveValues: {}, elementStates});

        render(<CommunicationProviderDetailsPageTest/>);

        await screen.findByTestId('element-derivation-context');
        fireEvent.click(screen.getByRole('button', {name: 'Kommunikationsanbieter testen'}));

        await waitFor(() => {
            expect(testState.showErrorSnackbar).toHaveBeenCalledWith('Bitte überprüfen Sie Ihre Eingaben.');
        });
        expect(testProvider).not.toHaveBeenCalled();
        expect(testState.derivationContextProps?.computedErrors).toEqual(elementStates);
    });

    it('allows an input-free test when no testing layout exists', async () => {
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout')
            .mockResolvedValue(null);
        const testProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider')
            .mockResolvedValue();
        const derive = vi.spyOn(ElementsApiService.prototype, 'derive');

        render(<CommunicationProviderDetailsPageTest/>);

        await screen.findByText('Für diesen Kommunikationsanbieter sind keine zusätzlichen Testeingaben erforderlich.');
        expect(screen.queryByTestId('element-derivation-context')).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Kommunikationsanbieter testen'}));

        await waitFor(() => expect(testProvider).toHaveBeenCalledWith(7, {}));
        expect(derive).not.toHaveBeenCalled();
    });

    it('does not load or test an unsaved provider', () => {
        testState.provider = {...createProvider(), id: 0};
        testState.isNewItem = true;
        const getLayout = vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout');
        const testProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider');

        render(<CommunicationProviderDetailsPageTest/>);

        expect(screen.getByText('Dieser Tab ist erst nach dem Anlegen des Kommunikationsanbieters verfügbar.'))
            .toBeInTheDocument();
        expect(getLayout).not.toHaveBeenCalled();
        expect(testProvider).not.toHaveBeenCalled();
    });

    it('keeps testing unavailable after a layout error and supports retrying the load', async () => {
        const loadError = new Error('layout failed');
        const getLayout = vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout')
            .mockRejectedValueOnce(loadError)
            .mockResolvedValueOnce(testingLayout);
        const testProvider = vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider');

        render(<CommunicationProviderDetailsPageTest/>);

        await screen.findByText('Laden Sie die Testoberfläche erneut, bevor Sie den Test durchführen.');
        expect(screen.queryByRole('button', {name: 'Kommunikationsanbieter testen'})).not.toBeInTheDocument();
        expect(testProvider).not.toHaveBeenCalled();
        expect(testState.showApiErrorSnackbar)
            .toHaveBeenCalledWith(loadError, 'Testoberfläche konnte nicht geladen werden.');

        fireEvent.click(screen.getByRole('button', {name: 'Erneut laden'}));

        await screen.findByTestId('element-derivation-context');
        expect(getLayout).toHaveBeenCalledTimes(2);
    });

    it('shows the API error when the provider test fails', async () => {
        const testError = new Error('test failed');
        vi.spyOn(CommunicationProvidersApiService.prototype, 'getProviderTestingLayout')
            .mockResolvedValue(null);
        vi.spyOn(CommunicationProvidersApiService.prototype, 'testProvider')
            .mockRejectedValue(testError);

        render(<CommunicationProviderDetailsPageTest/>);

        await screen.findByText('Für diesen Kommunikationsanbieter sind keine zusätzlichen Testeingaben erforderlich.');
        fireEvent.click(screen.getByRole('button', {name: 'Kommunikationsanbieter testen'}));

        await waitFor(() => {
            expect(testState.showApiErrorSnackbar)
                .toHaveBeenCalledWith(testError, 'Kommunikationsanbieter konnte nicht getestet werden.');
        });
    });
});

function createProvider(): CommunicationProvider {
    return {
        id: 7,
        communicationProviderDefinitionKey: 'mail',
        communicationProviderDefinitionVersion: 1,
        name: 'Test-Mail',
        description: 'Test provider',
        configuration: {},
        isEnabled: false,
        isTestProvider: true,
    };
}
