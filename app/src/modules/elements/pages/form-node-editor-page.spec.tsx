import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {FormNodeEditorPage} from './form-node-editor-page';
import {ProcessNodeApiService} from '../../process/services/process-node-api-service';
import {ProcessDefinitionApiService} from '../../process/services/process-definition-api-service';
import {ProcessDefinitionVersionApiService} from '../../process/services/process-definition-version-api-service';
import {ProcessTestClaimApiService} from '../../process/services/process-test-claim-api-service';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {SearchItemService} from '../../search/search-item-service';
import {FormTriggerApiService} from '../../forms/services/form-trigger-api-service';
import {XdfApiService} from '../../xdf/v1/xdf-api-service';
import {ElementType} from '../../../data/element-type/element-type';
import {ProcessStatus} from '../../process/enums/process-status';

const mocks = vi.hoisted(() => ({
    confirm: vi.fn(),
    dispatch: vi.fn(),
    downloadBlobFile: vi.fn(),
    hasChanged: false,
    submitValues: {} as Record<string, unknown>,
    uploadTextFile: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...await importOriginal<typeof import('react-router-dom')>(),
    useNavigate: () => vi.fn(),
    useParams: () => ({nodeId: '1'}),
    useSearchParams: () => [new URLSearchParams()],
}));

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({
        adminSettings: {
            devToolsTab: undefined,
            disableAutoScrollForSteps: false,
            disableElementContextMenu: false,
            hideComponentTree: true,
        },
        app: {
            showDialog: undefined,
        },
    }),
}));

vi.mock('../../../providers/confirm-provider', () => ({
    useConfirm: () => mocks.confirm,
}));

vi.mock('../../../hooks/use-api', () => ({
    useApi: () => ({}),
}));

vi.mock('../../../hooks/use-change-blocker-2', () => ({
    useChangeBlocker: () => ({
        dialog: null,
        hasChanged: mocks.hasChanged,
    }),
}));

vi.mock('../../../hooks/use-element-editor-navigation', () => ({
    useElementEditorNavigation: () => ({
        navigateToElementEditor: vi.fn(),
    }),
}));

vi.mock('../../../hooks/use-not-implemented', () => ({
    useNotImplemented: () => vi.fn(),
}));

vi.mock('../../../utils/element-size', () => ({
    useElementSize: () => ({
        ref: {current: null},
        size: {height: 800, width: 1200},
    }),
}));

vi.mock('../../../utils/download-utils', async (importOriginal) => ({
    ...await importOriginal<typeof import('../../../utils/download-utils')>(),
    downloadBlobFile: mocks.downloadBlobFile,
    uploadTextFile: mocks.uploadTextFile,
}));

vi.mock('allotment', async () => {
    const Allotment = ({children}: {children?: React.ReactNode}) => <div>{children}</div>;
    Allotment.Pane = ({children}: {children?: React.ReactNode}) => <div>{children}</div>;
    return {Allotment};
});

vi.mock('../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children?: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../../components/code-editor/code-editor', () => ({CodeEditor: () => null}));

vi.mock('../../../components/generic-page-header/generic-page-header', () => ({
    GenericPageHeader: ({actions}: {actions: any[]}) => (
        <div>
            {actions
                .filter((action) => action !== 'separator')
                .map((action) => (
                    <button
                        key={action.label ?? action.tooltip}
                        type="button"
                        disabled={action.disabled}
                        onClick={action.onClick}
                    >
                        {action.label ?? action.tooltip}
                    </button>
                ))}
        </div>
    ),
}));

vi.mock('../../forms/pages/details/components/form-details-page-more-menu', () => ({
    FormDetailsPageMoreMenu: ({items}: {items: any[]}) => (
        <div>
            {items
                .filter((item) => item !== 'separator')
                .map((item) => (
                    <button
                        key={item.label}
                        type="button"
                        disabled={item.disabled}
                        onClick={item.onClick ?? item.onToggle}
                    >
                        {item.label}
                    </button>
                ))}
        </div>
    ),
}));

vi.mock('../components/element-derivation-context', () => ({
    ElementDerivationContext: ({onEvent}: {onEvent: (values: Record<string, unknown>, event: string) => Promise<void>}) => (
        <button
            type="button"
            onClick={() => {
                void onEvent(mocks.submitValues, 'submit');
            }}
        >
            Testformular absenden
        </button>
    ),
}));

vi.mock('../../../components/submitted/submitted', () => ({
    Submitted: ({startedProcessAccessKey}: {startedProcessAccessKey: string}) => (
        <div>{`Übermittelt: ${startedProcessAccessKey}`}</div>
    ),
}));

vi.mock('../../../components/element-tree-2/element-tree', () => ({ElementTree: () => null}));
vi.mock('../../../components/developer-tools/developer-tools', () => ({DeveloperTools: () => null}));
vi.mock('../../../components/form/form-header-component', () => ({FormHeaderComponent: () => null}));
vi.mock('../../../components/form/root-component-footer', () => ({RootComponentFooter: () => null}));
vi.mock('../../../components/element-tree-2/components/element-tree-inline-editor-context', () => ({
    ElementTreeInlineEditorContextProvider: ({children}: {children?: React.ReactNode}) => <>{children}</>,
}));
vi.mock('../../../components/form/root-structure-actions-context', () => ({
    RootStructureActionsContextProvider: ({children}: {children?: React.ReactNode}) => <>{children}</>,
}));
vi.mock('../../../dialogs/add-element-dialog/add-element-dialog', () => ({AddElementDialog: () => null}));
vi.mock('../../../dialogs/prefill-form-dialog/prefill-form-dialog', () => ({PrefillFormDialog: () => null}));
vi.mock('../../../dialogs/help-dialog/help.dialog', () => ({HelpDialog: () => null, HelpDialogId: 'help'}));
vi.mock('../../../dialogs/privacy-dialog/privacy-dialog', () => ({PrivacyDialog: () => null, PrivacyDialogId: 'privacy'}));
vi.mock('../../../dialogs/imprint-dialog/imprint-dialog', () => ({ImprintDialog: () => null, ImprintDialogId: 'imprint'}));
vi.mock('../../../dialogs/accessibility-dialog/accessibility-dialog', () => ({AccessibilityDialog: () => null, AccessibilityDialogId: 'accessibility'}));
vi.mock('../../../components/dialog-title-with-close/dialog-title-with-close', () => ({DialogTitleWithClose: () => null}));
vi.mock('../../identity/components/identity-button/identity-button', () => ({IdentityButton: () => null}));
vi.mock('../../payment/components/payment-request-overview', () => ({PaymentRequestOverview: () => null}));

describe('FormNodeEditorPage error handling', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    beforeEach(() => {
        mocks.confirm.mockReset().mockResolvedValue(true);
        mocks.dispatch.mockReset();
        mocks.downloadBlobFile.mockReset();
        mocks.hasChanged = false;
        mocks.submitValues = {};
        mocks.uploadTextFile.mockReset().mockResolvedValue('<xdf/>');

        Object.defineProperty(window, 'matchMedia', {
            configurable: true,
            value: vi.fn().mockReturnValue({
                addEventListener: vi.fn(),
                matches: false,
                removeEventListener: vi.fn(),
            }),
        });

        vi.spyOn(ProcessNodeApiService.prototype, 'retrieve').mockResolvedValue(createNode());
        vi.spyOn(ProcessNodeApiService.prototype, 'update').mockResolvedValue(createNode());
        vi.spyOn(ProcessDefinitionApiService.prototype, 'retrieve').mockResolvedValue(createProcess());
        vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'retrieve').mockResolvedValue(createProcessVersion());
        vi.spyOn(ProcessTestClaimApiService.prototype, 'listAll').mockResolvedValue(createPage([createTestClaim()]));
        vi.spyOn(ProcessTestClaimApiService.prototype, 'create').mockResolvedValue(createTestClaim());
        vi.spyOn(IdentityProvidersApiService.prototype, 'listAll').mockResolvedValue(createPage([]));
        vi.spyOn(SearchItemService.prototype, 'recordRecentSearchItem').mockResolvedValue(undefined);
        vi.spyOn(FormTriggerApiService.prototype, 'getFormTheme').mockResolvedValue(createTheme());
        vi.spyOn(FormTriggerApiService.prototype, 'calculateCosts').mockResolvedValue({
            hasTaxes: false,
            paymentItems: [],
            paymentProviderName: '',
            totalCost: 0,
        });
        vi.spyOn(FormTriggerApiService.prototype, 'submitForm').mockResolvedValue({
            startedProcessAccessKey: 'started-process',
        });
        vi.spyOn(XdfApiService.prototype, 'xdfTransform').mockResolvedValue(createFormLayout());
    });

    it('sets a generic shell error when essential editor loading fails unexpectedly', async () => {
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockRejectedValue(new Error('broken response'));

        render(<FormNodeEditorPage/>);

        await waitFor(() => expectDispatchedAction('shell/setErrorMessage', {
            message: 'Der Formulareditor konnte nicht geladen werden.',
            status: 500,
        }));
    });

    it('sets a displayable shell error when related process loading returns one', async () => {
        vi.mocked(ProcessDefinitionApiService.prototype.retrieve).mockRejectedValue({
            status: 403,
            message: 'Der Prozess darf nicht geöffnet werden.',
            details: null,
            displayableToUser: true,
        });

        render(<FormNodeEditorPage/>);

        await waitFor(() => expectDispatchedAction('shell/setErrorMessage', {
            message: 'Der Prozess darf nicht geöffnet werden.',
            status: 403,
        }));
    });

    it('shows a displayable API error when saving from the page header fails', async () => {
        mocks.hasChanged = true;
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValue({
            status: 409,
            message: 'Das Formular wurde zwischenzeitlich geändert.',
            details: null,
            displayableToUser: true,
        });
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expectSnackbar('Das Formular wurde zwischenzeitlich geändert.'));
    });

    it('aborts submission when saving pending editor changes fails', async () => {
        mocks.hasChanged = true;
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValue(new Error('save failed'));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Fehler beim Speichern der Änderungen'));
        expect(FormTriggerApiService.prototype.calculateCosts).not.toHaveBeenCalled();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
    });

    it('always clears the loading overlay when XDF transformation fails', async () => {
        vi.mocked(XdfApiService.prototype.xdfTransform).mockRejectedValue(new Error('invalid XDF'));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'XDatenfeld-Schema importieren'}));

        await waitFor(() => expectSnackbar('Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
        expectDispatchedAction('shell/setLoadingMessage', {
            blocking: true,
            estimatedTime: 1000,
            message: 'Importiere XDF',
        });
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });

    it('uses the localized fallback for a non-displayable cost API error and aborts submission', async () => {
        vi.mocked(FormTriggerApiService.prototype.calculateCosts).mockRejectedValue({
            status: 500,
            message: 'Internal Server Error',
            details: null,
            displayableToUser: false,
        });
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Beim Berechnen der Kosten ist ein unbekannter Fehler aufgetreten.'));
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('reports test-mode preparation failures and does not calculate costs', async () => {
        vi.mocked(ProcessTestClaimApiService.prototype.listAll)
            .mockResolvedValueOnce(createPage([createTestClaim()]))
            .mockRejectedValueOnce(new Error('test claim lookup failed'));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Der Testmodus für das Formular konnte nicht vorbereitet werden.'));
        expect(FormTriggerApiService.prototype.calculateCosts).not.toHaveBeenCalled();
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('reports attachment preparation failures without starting submission loading', async () => {
        const fileLayout = createFormLayout([
            {
                id: 'attachment',
                type: ElementType.FileUpload,
                children: [],
            },
        ]);
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(createNode(fileLayout));
        mocks.submitValues = {
            attachment: [{
                name: 'attachment.pdf',
                size: 100,
                uri: 'blob:missing',
            }],
        };
        vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Die Anhänge konnten nicht für das Absenden vorbereitet werden.'));
        expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
        expectActionTypeNotDispatched('shell/setLoadingMessage');
    });

    it('shows submission errors and clears the blocking loading overlay', async () => {
        vi.mocked(FormTriggerApiService.prototype.submitForm).mockRejectedValue(new Error('submission failed'));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Beim Absenden des Formulars ist ein Fehler aufgetreten'));
        expectDispatchedAction('shell/setLoadingMessage', {
            blocking: true,
            estimatedTime: 1000,
            message: 'Formular wird abgesendet',
        });
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });

    it('keeps the successful submission behavior and clears loading', async () => {
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        expect(await screen.findByText('Übermittelt: started-process')).toBeVisible();
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });
});

async function renderLoadedEditor(): Promise<void> {
    render(<FormNodeEditorPage/>);
    await screen.findByRole('button', {name: 'Testformular absenden'});
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

function createNode(formLayout = createFormLayout()): any {
    return {
        configuration: {
            formLayout,
            formSlug: 'test-form',
        },
        id: 1,
        name: 'Formulareingang',
        processId: 10,
        processVersion: 1,
    };
}

function createProcess(): any {
    return {
        id: 10,
        internalTitle: 'Testprozess',
        slug: 'test-process',
    };
}

function createProcessVersion(): any {
    return {
        processId: 10,
        processVersion: 1,
        status: ProcessStatus.Drafted,
        themeId: null,
    };
}

function createTestClaim(): any {
    return {
        accessKey: 'test-claim',
        id: 1,
        processId: 10,
        processVersion: 1,
    };
}

function createTheme(): any {
    return {
        faviconKey: null,
        id: 1,
        logoKey: null,
        logoKeyDark: null,
        name: 'Test theme',
        primaryColor: '#005ea8',
        primaryColorDark: null,
        secondaryColor: '#1f2937',
        secondaryColorDark: null,
    };
}

function createPage<T>(content: T[]): any {
    return {
        content,
        page: 0,
        size: content.length,
        totalElements: content.length,
        totalPages: 1,
    };
}
