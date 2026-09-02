import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {CustomerFormPage} from './customer-form-page';
import {BaseApiService} from '../../services/base-api-service';
import {CustomerInputService} from '../../services/customer-input-service';
import {FormTriggerApiService} from '../../modules/forms/services/form-trigger-api-service';
import {ElementType} from '../../data/element-type/element-type';
import {ProcessStatus} from '../../modules/process/enums/process-status';

const mocks = vi.hoisted(() => ({
    confirm: vi.fn(),
    dispatch: vi.fn(),
    eventResolved: vi.fn(),
    navigate: vi.fn(),
    submitValues: {} as Record<string, unknown>,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-router-dom')>(),
    useNavigate: () => mocks.navigate,
    useParams: () => ({
        processSlug: 'test-process',
        formSlug: 'test-form',
    }),
    useSearchParams: () => [new URLSearchParams()],
}));

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../hooks/use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({
        app: {
            showDialog: undefined,
        },
        systemConfig: {
            ProviderName: 'Test provider',
        },
    }),
}));

vi.mock('../../providers/confirm-provider', () => ({
    useConfirm: () => mocks.confirm,
}));

vi.mock('notistack', () => ({
    SnackbarProvider: ({children}: {children?: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../components/code-editor/code-editor', () => ({CodeEditor: () => null}));

vi.mock('../../modules/elements/components/element-derivation-context', () => ({
    ElementDerivationContext: function ElementDerivationContextMock({
        onEvent,
    }: {
        onEvent: (values: Record<string, unknown>, event: string) => Promise<void>;
    }) {
        const [isBusy, setIsBusy] = React.useState(false);

        return (
            <button
                type="button"
                disabled={isBusy}
                onClick={() => {
                    setIsBusy(true);
                    void onEvent(mocks.submitValues, 'submit')
                        .then(() => {
                            mocks.eventResolved();
                            setIsBusy(false);
                        });
                }}
            >
                Formular absenden
            </button>
        );
    },
}));

vi.mock('../../dialogs/customer-input-loader/customer-input-loader', () => ({
    CustomerInputLoader: function CustomerInputLoaderMock({onResolved}: {onResolved: () => void}) {
        React.useEffect(() => {
            onResolved();
        }, [onResolved]);

        return null;
    },
}));

vi.mock('../../components/meta-element/meta-element', () => ({MetaElement: () => null}));
vi.mock('../../components/form/form-header-component', () => ({FormHeaderComponent: () => null}));
vi.mock('../../components/form/root-component-footer', () => ({RootComponentFooter: () => null}));
vi.mock('../../components/submitted/submitted', () => ({
    Submitted: ({startedProcessAccessKey}: {startedProcessAccessKey: string}) => (
        <div>{`Übermittelt: ${startedProcessAccessKey}`}</div>
    ),
}));
vi.mock('../../dialogs/help-dialog/help.dialog', () => ({HelpDialog: () => null, HelpDialogId: 'help'}));
vi.mock('../../dialogs/privacy-dialog/privacy-dialog', () => ({PrivacyDialog: () => null, PrivacyDialogId: 'privacy'}));
vi.mock('../../dialogs/imprint-dialog/imprint-dialog', () => ({ImprintDialog: () => null, ImprintDialogId: 'imprint'}));
vi.mock('../../dialogs/accessibility-dialog/accessibility-dialog', () => ({AccessibilityDialog: () => null, AccessibilityDialogId: 'accessibility'}));
vi.mock('../../modules/payment/components/payment-request-overview', () => ({PaymentRequestOverview: () => null}));

describe('CustomerFormPage error handling', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    beforeEach(() => {
        mocks.confirm.mockReset().mockResolvedValue(true);
        mocks.dispatch.mockReset();
        mocks.eventResolved.mockReset();
        mocks.navigate.mockReset();
        mocks.submitValues = {};

        Object.defineProperty(window, 'matchMedia', {
            configurable: true,
            value: vi.fn().mockReturnValue({
                addEventListener: vi.fn(),
                matches: false,
                removeEventListener: vi.fn(),
            }),
        });

        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.spyOn(BaseApiService.prototype, 'get').mockResolvedValue(createRetrieveResponse());
        vi.spyOn(FormTriggerApiService.prototype, 'getFormTheme').mockResolvedValue(undefined as never);
        vi.spyOn(FormTriggerApiService.prototype, 'calculateCosts').mockResolvedValue(createCosts());
        vi.spyOn(FormTriggerApiService.prototype, 'submitForm').mockResolvedValue({
            startedProcessAccessKey: 'started-process',
        });
        vi.spyOn(CustomerInputService, 'loadCustomerInputDraft').mockReturnValue(null);
        vi.spyOn(CustomerInputService, 'cleanCustomerInput').mockImplementation(() => undefined);
    });

    it('sets a generic shell error when form loading fails unexpectedly', async () => {
        vi.mocked(BaseApiService.prototype.get).mockRejectedValue(new Error('broken response'));

        render(<CustomerFormPage/>);

        await waitFor(() => expectDispatchedAction('shell/setErrorMessage', {
            message: 'Das Formular konnte nicht geladen werden.',
            status: 500,
        }));
    });

    it('sets a displayable shell error when form loading returns one', async () => {
        vi.mocked(BaseApiService.prototype.get).mockRejectedValue({
            status: 403,
            message: 'Das Formular darf nicht geöffnet werden.',
            details: null,
            displayableToUser: true,
        });

        render(<CustomerFormPage/>);

        await waitFor(() => expectDispatchedAction('shell/setErrorMessage', {
            message: 'Das Formular darf nicht geöffnet werden.',
            status: 403,
        }));
    });

    it('uses the localized fallback for a non-displayable cost API error', async () => {
        vi.mocked(FormTriggerApiService.prototype.calculateCosts).mockRejectedValue({
            status: 500,
            message: 'Internal Server Error',
            details: null,
            displayableToUser: false,
        });
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Beim Berechnen der Kosten ist ein unbekannter Fehler aufgetreten.'));
        await expectSubmissionReleased();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('shows a displayable cost API error', async () => {
        vi.mocked(FormTriggerApiService.prototype.calculateCosts).mockRejectedValue({
            status: 422,
            message: 'Die Gebühren konnten für diese Angaben nicht ermittelt werden.',
            details: null,
            displayableToUser: true,
        });
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Die Gebühren konnten für diese Angaben nicht ermittelt werden.'));
        await expectSubmissionReleased();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
    });

    it('reports payment preparation failures and releases submission', async () => {
        vi.mocked(FormTriggerApiService.prototype.calculateCosts).mockResolvedValue(createCosts(12));
        mocks.confirm.mockRejectedValue(new Error('dialog failed'));
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Die Zahlungsanforderung konnte nicht vorbereitet werden.'));
        await expectSubmissionReleased();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('reports attachment network failures without starting submission loading', async () => {
        prepareAttachmentForm();
        vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Die Anhänge konnten nicht für das Absenden vorbereitet werden.'));
        await expectSubmissionReleased();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('reports non-success attachment responses without reading their bodies', async () => {
        prepareAttachmentForm();
        const blob = vi.fn();
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            blob,
            ok: false,
            status: 404,
        }));
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Die Anhänge konnten nicht für das Absenden vorbereitet werden.'));
        await expectSubmissionReleased();
        expect(blob).not.toHaveBeenCalled();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
    });

    it('shows submission errors, clears loading, and releases submission', async () => {
        vi.mocked(FormTriggerApiService.prototype.submitForm).mockRejectedValue({
            status: 500,
            message: 'Internal Server Error',
            details: null,
            displayableToUser: false,
        });
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        await waitFor(() => expectSnackbar('Beim Absenden des Formulars ist ein Fehler aufgetreten.'));
        await expectSubmissionReleased();
        expectDispatchedAction('shell/setLoadingMessage', {
            blocking: true,
            estimatedTime: 1000,
            message: 'Formular wird abgesendet',
        });
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });

    it('keeps successful submission behavior and clears loading', async () => {
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        expect(await screen.findByText('Übermittelt: started-process')).toBeVisible();
        await waitFor(() => expect(mocks.eventResolved).toHaveBeenCalledOnce());
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });

    it('keeps a successful submission successful when local draft cleanup fails', async () => {
        const cleanupError = new Error('storage unavailable');
        vi.mocked(CustomerInputService.cleanCustomerInput).mockImplementation(() => {
            throw cleanupError;
        });
        await renderLoadedForm();

        fireEvent.click(screen.getByRole('button', {name: 'Formular absenden'}));

        expect(await screen.findByText('Übermittelt: started-process')).toBeVisible();
        await waitFor(() => expect(mocks.eventResolved).toHaveBeenCalledOnce());
        expect(console.error).toHaveBeenCalledWith('Error cleaning submitted customer input:', cleanupError);
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });
});

async function renderLoadedForm(): Promise<void> {
    render(<CustomerFormPage/>);
    await screen.findByRole('button', {name: 'Formular absenden'});
}

async function expectSubmissionReleased(): Promise<void> {
    await waitFor(() => expect(mocks.eventResolved).toHaveBeenCalledOnce());
    expect(screen.getByRole('button', {name: 'Formular absenden'})).toBeEnabled();
}

function prepareAttachmentForm(): void {
    vi.mocked(BaseApiService.prototype.get).mockResolvedValue(createRetrieveResponse(createFormLayout([
        {
            id: 'attachment',
            type: ElementType.FileUpload,
            children: [],
        },
    ])));
    mocks.submitValues = {
        attachment: [{
            name: 'attachment.pdf',
            size: 100,
            uri: 'blob:attachment',
        }],
    };
}

function expectSnackbar(message: string): void {
    expectDispatchedAction('shell/addSnackbarMessage', expect.objectContaining({message}));
}

function expectActionTypeDispatched(type: string): void {
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({type}));
}

function expectActionTypeNotDispatched(type: string): void {
    expect(mocks.dispatch).not.toHaveBeenCalledWith(expect.objectContaining({type}));
}

function expectDispatchedAction(type: string, payload: unknown): void {
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
        payload,
        type,
    }));
}

function createRetrieveResponse(layoutElement = createFormLayout()): any {
    return {
        identitySlots: [],
        layoutElement,
        node: {
            configuration: {
                formSlug: 'test-form',
            },
            id: 1,
            name: 'Formulareingang',
            processId: 10,
            processVersion: 1,
        },
        process: {
            id: 10,
            internalTitle: 'Testprozess',
            slug: 'test-process',
        },
        version: {
            processId: 10,
            processVersion: 1,
            status: ProcessStatus.Published,
            themeId: null,
        },
    };
}

function createFormLayout(children: any[] = []): any {
    return {
        children,
        id: 'form',
        managingDepartmentId: null,
        publicTitle: 'Testformular',
        responsibleDepartmentId: null,
        type: ElementType.FormLayout,
    };
}

function createCosts(totalCost = 0): any {
    return {
        hasTaxes: false,
        paymentItems: [],
        paymentProviderName: '',
        totalCost,
    };
}
