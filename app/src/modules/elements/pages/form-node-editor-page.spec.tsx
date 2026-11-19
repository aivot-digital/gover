import {createTheme as createMuiTheme, ThemeProvider} from '@mui/material';
import React from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryRouter, Link, RouterProvider} from 'react-router-dom';
import {AuthService} from '../../../services/auth-service';
import {type EditorDraft, EditorDraftStorage} from '../../../services/editor-draft-storage';
import {initializeEditorDraftLoginReturn, prepareEditorDraftLogin, EDITOR_DRAFT_RETURN_PARAM} from '../../../services/editor-draft-login';
import {DragAndDropProvider} from '../../../providers/drag-and-drop-provider';
import {ElementsApiService} from '../elements-api-service';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
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
import {literalAuthoredValue} from '../../../models/element-data';

const mocks = vi.hoisted(() => ({
    api: {},
    confirm: vi.fn(),
    devToolsTab: undefined as number | undefined,
    dispatch: vi.fn(),
    downloadBlobFile: vi.fn(),
    withRouter: false,
    withRealTree: false,
    replaceAuthoredElementValues: vi.fn(),
    observeScrollContainer: vi.fn(),
    submitValues: {} as Record<string, unknown>,
    uploadTextFile: vi.fn(),
    canUpdateProcess: true,
    onDerive: undefined as undefined | ((values: Record<string, unknown>, derivationOptions: unknown, abort?: AbortSignal) => Promise<unknown>),
}));

vi.mock('../../permissions/hooks/use-permissions', async (importOriginal) => ({
    ...await importOriginal<typeof import('../../permissions/hooks/use-permissions')>(),
    useHasProcessPermission: () => mocks.canUpdateProcess,
}));

vi.mock('react-router-dom', async (importOriginal) => {
    const router = await importOriginal<typeof import('react-router-dom')>();
    return {
        ...router,
        useNavigate: () => mocks.withRouter ? router.useNavigate() : vi.fn(),
        useParams: () => ({nodeId: '1'}),
        useSearchParams: () => [new URLSearchParams()],
    };
});

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => selector({
        adminSettings: {
            devToolsTab: mocks.devToolsTab,
            disableAutoScrollForSteps: false,
            disableElementContextMenu: false,
            hideComponentTree: !mocks.withRouter,
        },
        app: {
            showDialog: undefined,
        },
        user: {
            user: mocks.withRouter ? {id: 'staff-user'} : undefined,
        },
    }),
}));

vi.mock('../../../providers/confirm-provider', () => ({
    useConfirm: () => mocks.confirm,
}));

vi.mock('../../../hooks/use-api', () => ({
    // Stable like the memoized API of the real hook; a new object per render would rerun every effect that depends on it.
    useApi: () => mocks.api,
}));

vi.mock('../../../hooks/use-change-blocker-2', async (importOriginal) => {
    const {useChangeBlocker} = await importOriginal<typeof import('../../../hooks/use-change-blocker-2')>();
    return {
        useChangeBlocker: (props: Parameters<typeof useChangeBlocker>[0]) => mocks.withRouter
            ? useChangeBlocker(props)
            : {dialog: null, hasChanged: false},
    };
});

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
vi.mock('../../../editors', () => ({editors: {}}));

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
    ElementDerivationContext: React.forwardRef((
        {onEvent, onDerive, scrollContainerRef}: {
            onEvent: (values: Record<string, unknown>, event: string) => Promise<void>;
            onDerive?: (values: Record<string, unknown>, derivationOptions: unknown, abort?: AbortSignal) => Promise<unknown>;
            scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
        },
        ref,
    ) => {
        mocks.observeScrollContainer(scrollContainerRef);
        mocks.onDerive = onDerive;
        React.useImperativeHandle(ref, () => ({
            replaceAuthoredElementValues: mocks.replaceAuthoredElementValues,
        }));

        return (
            <button
                type="button"
                onClick={() => {
                    void onEvent(mocks.submitValues, 'submit');
                }}
            >
                Testformular absenden
            </button>
        );
    }),
}));

vi.mock('../../../components/submitted/submitted', () => ({
    Submitted: ({startedProcessAccessKey}: {startedProcessAccessKey: string}) => (
        <div>{`Übermittelt: ${startedProcessAccessKey}`}</div>
    ),
}));

vi.mock('../../../components/element-tree-2/element-tree', async (importOriginal) => {
    const {ElementTree} = await importOriginal<typeof import('../../../components/element-tree-2/element-tree')>();
    return {
        ElementTree: (props: React.ComponentProps<typeof ElementTree<FormLayoutElement>>) =>
            mocks.withRealTree ? <ElementTree {...props}/> : (
                <input aria-label="Formulartitel" value={props.value.publicTitle ?? ''}
                    onChange={(event) => props.onChange({...props.value, publicTitle: event.target.value})}/>
            ),
    };
});
vi.mock('../../../components/developer-tools/developer-tools', () => ({
    DeveloperTools: ({onElementDataChange}: {
        onElementDataChange: (elementData: Record<string, unknown>) => void;
    }) => (
        <button
            type="button"
            onClick={() => onElementDataChange({field: 'imported'})}
        >
            Formulareingaben importieren
        </button>
    ),
}));
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

describe('FormNodeEditorPage', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    beforeEach(() => {
        mocks.confirm.mockReset().mockResolvedValue(true);
        mocks.devToolsTab = undefined;
        mocks.dispatch.mockReset();
        mocks.downloadBlobFile.mockReset();
        window.history.replaceState({}, '', '/');
        initializeEditorDraftLoginReturn();
        mocks.withRouter = false;
        mocks.withRealTree = false;
        mocks.replaceAuthoredElementValues.mockReset().mockResolvedValue(undefined);
        mocks.submitValues = {};
        mocks.uploadTextFile.mockReset().mockResolvedValue('<xdf/>');
        mocks.canUpdateProcess = true;
        mocks.onDerive = undefined;

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
        vi.spyOn(ProcessNodeApiService.prototype, 'deriveUiDefinition').mockResolvedValue({
            effectiveValues: {},
            elementStates: {},
        });
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

    it.each(['Übernehmen', 'Abbrechen'])('restores the pending title in the same element editor until %s', async (action) => {
        const {save, returnFromLogin} = await renderOpenTextElement();
        const title = () => screen.getByRole('textbox', {name: /^Titel/});

        if (action === 'Abbrechen') {
            fireEvent.change(title(), {target: {value: 'Bereits übernommen'}});
            fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
            fireEvent.click(await screen.findByText('Bereits übernommen'));
        }
        fireEvent.change(title(), {target: {value: 'Noch offen'}});
        if (action === 'Abbrechen') {
            fireEvent.click(screen.getByRole('tab', {name: 'Elementstruktur'}));
        }

        await returnFromLogin();
        const priorTitle = action === 'Abbrechen' ? 'Bereits übernommen' : 'Vorname';
        expect(save).toHaveBeenLastCalledWith(expect.objectContaining({
            mine: expect.objectContaining({children: [expect.objectContaining({children: [expect.objectContaining({label: priorTitle})]})]}),
            context: expect.objectContaining({mine: expect.objectContaining({label: 'Noch offen'})}),
        }));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen wiederherstellen'}));
        if (action === 'Abbrechen') {
            expect(await screen.findByRole('tab', {name: 'Elementstruktur', selected: true})).toBeVisible();
            fireEvent.click(screen.getByRole('tab', {name: 'Eigenschaften'}));
        }
        expect(await screen.findByRole('textbox', {name: /^Titel/})).toHaveValue('Noch offen');
        fireEvent.click(screen.getByRole('button', {name: action}));
        const finalTitle = action === 'Abbrechen' ? priorTitle : 'Noch offen';
        expect(await screen.findByText(finalTitle)).toBeVisible();
        expect(await screen.findByRole('button', {name: 'Speichern'})).toBeEnabled();
        await prepareEditorDraftLogin();
        const draft = save.mock.lastCall?.[0];
        expect(draft?.context).toBeUndefined();
        expect(draft?.mine).toMatchObject({children: [{children: [{label: finalTitle}]}]});
        expect(ProcessNodeApiService.prototype.update).not.toHaveBeenCalled();
    });

    it('keeps no draft for opening an element, changing tabs or reverting the title', async () => {
        const {save} = await renderOpenTextElement();
        fireEvent.click(screen.getByRole('tab', {name: 'Elementstruktur'}));
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        fireEvent.click(screen.getByRole('tab', {name: 'Eigenschaften'}));
        const title = screen.getByRole('textbox', {name: /^Titel/});
        fireEvent.change(title, {target: {value: 'Zwischenstand'}});
        fireEvent.change(title, {target: {value: 'Vorname'}});
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        expect(save).not.toHaveBeenCalled();
    });

    it('discards a panel-only draft on login and closes its original deep link', async () => {
        const {returnFromLogin, remove} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Verwerfen'}});
        await returnFromLogin('#first-name/properties');
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));

        await waitFor(() => expect(window.location.hash).toBe(''));
        expect(await screen.findByRole('button', {name: 'Speichern'})).toBeDisabled();
        expect(screen.queryByRole('textbox', {name: /^Titel/})).not.toBeInTheDocument();
        expect(remove).toHaveBeenCalled();
        expect(await prepareEditorDraftLogin()).toBeUndefined();
    });

    it.each(['Abbrechen', 'Übernehmen'])('keeps reinsertion of a deleted field pending until %s', async (action) => {
        const {returnFromLogin, save} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Eigener Titel'}});
        const remote = createFormLayout([{id: 'section', type: ElementType.Step, title: 'Abschnitt', children: []}]);
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(remote), lockVersion: 2});
        await returnFromLogin();
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen wiederherstellen'}));
        expect(await screen.findByRole('textbox', {name: /^Titel/})).toHaveValue('Eigener Titel');
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0].mine).toMatchObject({children: [{children: []}]});

        fireEvent.click(screen.getByRole('button', {name: action}));
        const saveForm = await screen.findByRole('button', {name: 'Speichern'});
        if (action === 'Abbrechen') {
            expect(saveForm).toBeDisabled();
            expect(await prepareEditorDraftLogin()).toBeUndefined();
        } else {
            expect(saveForm).toBeEnabled();
            await prepareEditorDraftLogin();
            expect(save.mock.lastCall?.[0].mine).toMatchObject({children: [{children: [{id: 'first-name', label: 'Eigener Titel'}]}]});
        }
    });

    it('blocks navigation for a panel-only edit and removes it after confirmed discard', async () => {
        const {router, load} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Offen'}});
        await act(async () => { await router.navigate('/processes/10/versions/1/nodes/1'); });
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        fireEvent.click(await screen.findByRole('link', {name: 'Formular öffnen'}));
        await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
        expect(await load.mock.results[1].value).toBeNull();
        expect(screen.queryByRole('button', {name: 'Änderungen wiederherstellen'})).not.toBeInTheDocument();
    });

    it.each([false, true])('discards the edited form before returning to the process (debounce elapsed: %s)', async (saved) => {
        const {load, remove, save} = await renderDraftEditor();
        const marker = `Discarded form title ${crypto.randomUUID()}`;
        fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: marker}});
        if (saved) {
            await waitFor(() => expect(save).toHaveBeenCalledWith(expect.objectContaining({
                mine: expect.objectContaining({publicTitle: marker}),
            })), {timeout: 3000});
        } else {
            expect(save).not.toHaveBeenCalled();
        }

        fireEvent.click(screen.getByRole('button', {name: 'Zurück zum Prozess'}));
        const commit = Promise.withResolvers<void>();
        const deleteRecord = remove.getMockImplementation()!;
        remove.mockImplementationOnce(async (...args) => {
            await commit.promise;
            await deleteRecord(...args);
        });
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        expect(screen.getByRole('button', {name: 'Änderungen verwerfen'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Abbrechen'})).toBeDisabled();
        expect(screen.queryByRole('link', {name: 'Formular öffnen'})).not.toBeInTheDocument();

        await act(async () => commit.resolve());
        fireEvent.click(await screen.findByRole('link', {name: 'Formular öffnen'}));
        await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
        expect(await load.mock.results[1].value).toBeNull();
        expect(await screen.findByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Testformular');
        expect(screen.queryByRole('button', {name: 'Änderungen wiederherstellen'})).not.toBeInTheDocument();
        if (!saved) {
            expect(save).not.toHaveBeenCalled();
        }
    });

    it('keeps the edited form open after a failed discard and lets the user retry', async () => {
        const {remove, save} = await renderDraftEditor();
        vi.spyOn(console, 'error').mockImplementation(() => {});
        fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: 'Lokale Änderung'}});
        await waitFor(() => expect(save).toHaveBeenCalled(), {timeout: 3000});
        remove.mockRejectedValueOnce(new DOMException('Transaction aborted', 'AbortError'));

        fireEvent.click(screen.getByRole('button', {name: 'Zurück zum Prozess'}));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        expect(await screen.findByRole('alert')).toHaveTextContent('Die Änderungen konnten nicht verworfen werden.');
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Lokale Änderung');
        fireEvent.click(screen.getByRole('button', {name: 'Zurück zum Prozess'}));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        expect(await screen.findByRole('link', {name: 'Formular öffnen'})).toBeInTheDocument();
    });

    it.each(['light', 'dark'] as const)('uses a neutral content surface in %s mode even with tinted surrounding surfaces', async mode => {
        const theme = createMuiTheme({palette: {mode, background: {default: '#ffeeee', paper: '#eeffee'}}});
        render(<ThemeProvider theme={theme}><FormNodeEditorPage/></ThemeProvider>);

        const submit = await screen.findByRole('button', {name: 'Testformular absenden'});
        expect(submit.parentElement).toHaveStyle({backgroundColor: mode === 'light' ? '#ffffff' : '#1c1c1c'});
    });

    it('passes the scrolling preview container to the form', async () => {
        render(<FormNodeEditorPage/>);
        const submit = await screen.findByRole('button', {name: 'Testformular absenden'});
        const scrollContainer = mocks.observeScrollContainer.mock.lastCall?.[0]?.current;
        expect(scrollContainer).toContainElement(submit);
        expect(scrollContainer).toHaveStyle({overflowY: 'auto'});
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
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValue({
            status: 409,
            message: 'Das Formular wurde zwischenzeitlich geändert.',
            details: null,
            displayableToUser: true,
        });
        await renderModifiedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expectSnackbar('Das Formular wurde zwischenzeitlich geändert.'));
    });

    it('saves again on top of a newer stored node when a save is rejected by its lock version', async () => {
        const storedNode = {...createNode(), name: 'Anderswo gespeichert', lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.update)
            .mockRejectedValueOnce({
                status: 409,
                message: 'Der Datensatz wurde zwischenzeitlich geändert.',
                details: {reason: 'lock_version_conflict'},
                displayableToUser: true,
            })
            .mockResolvedValue(storedNode);
        await renderModifiedEditor();
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(storedNode);

        fireEvent.click(screen.getByRole('button', {name: 'Speichern'}));

        await waitFor(() => expectSnackbar('Ihre Änderungen wurden mit dem zwischenzeitlich gespeicherten Stand zusammengeführt und gespeichert.'));
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(2);
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.calls[1][1]).toEqual(expect.objectContaining({
            name: 'Anderswo gespeichert',
            lockVersion: 2,
        }));
    });

    it('aborts submission when saving pending editor changes fails', async () => {
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValue(new Error('save failed'));
        await renderModifiedEditor();

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

    it('derives element data imported through the developer tools', async () => {
        mocks.devToolsTab = 0;
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Formulareingaben importieren'}));

        expect(mocks.replaceAuthoredElementValues).toHaveBeenCalledOnce();
        expect(mocks.replaceAuthoredElementValues).toHaveBeenCalledWith({field: 'imported'});
    });

    it('derives the unsaved form through the node endpoint when the process may be updated', async () => {
        await renderLoadedEditor();
        const values = {field: literalAuthoredValue('value')};
        const derivationOptions = createDerivationOptions();

        await mocks.onDerive?.(values, derivationOptions);

        expect(ProcessNodeApiService.prototype.deriveUiDefinition).toHaveBeenCalledWith(
            1,
            'formLayout',
            expect.objectContaining({type: ElementType.FormLayout}),
            values,
            derivationOptions,
            undefined,
        );
    });

    it('derives the stored form when the process may not be updated', async () => {
        mocks.canUpdateProcess = false;
        await renderLoadedEditor();
        const values = {field: literalAuthoredValue('value')};
        const derivationOptions = createDerivationOptions();

        await mocks.onDerive?.(values, derivationOptions);

        expect(ProcessNodeApiService.prototype.deriveUiDefinition).toHaveBeenCalledWith(
            1,
            'formLayout',
            null,
            values,
            derivationOptions,
            undefined,
        );
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
            attachment: literalAuthoredValue([{
                name: 'attachment.pdf',
                size: 100,
                uri: 'blob:missing',
            }]),
        };
        vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
        await renderLoadedEditor();

        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));

        await waitFor(() => expectSnackbar('Die Anlagen konnten nicht für das Absenden vorbereitet werden.'));
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

async function renderModifiedEditor(): Promise<void> {
    await renderDraftEditor();
    fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: 'Bearbeitet'}});
}

async function renderOpenTextElement() {
    mocks.withRealTree = true;
    const form = createFormLayout([{id: 'section', type: ElementType.Step, title: 'Abschnitt', children: [
        {id: 'first-name', type: ElementType.Text, label: 'Vorname'},
    ]}]);
    vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(createNode(form));
    vi.spyOn(ElementsApiService.prototype, 'recalculateReferencedIds').mockImplementation(async (value) => value);
    window.history.replaceState({}, '', '/form#first-name/properties');
    return renderDraftEditor();
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

async function renderDraftEditor() {
    mocks.withRouter = true;
    vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
    let stored: EditorDraft<unknown> | null = null;
    const load = vi.spyOn(EditorDraftStorage, 'load').mockImplementation(async <T, C>() => stored as EditorDraft<T, C> | null);
    const save = vi.spyOn(EditorDraftStorage, 'save').mockImplementation(async (draft) => {
        stored = {...draft, tabId: 'editor-tab'};
    });
    const remove = vi.spyOn(EditorDraftStorage, 'remove').mockImplementation(async () => {
        stored = null;
    });
    vi.spyOn(EditorDraftStorage, 'reserve').mockImplementation(async () => stored != null);
    const mount = () => {
        const router = createMemoryRouter([
            {path: '/form', element: <FormNodeEditorPage/>},
            {path: '/processes/10/versions/1/nodes/1', element: <Link to="/form">Formular öffnen</Link>},
        ], {initialEntries: ['/form']});
        const view = render(<DragAndDropProvider><RouterProvider router={router}/></DragAndDropProvider>);
        return {view, router};
    };
    const {view, router} = mount();
    await waitFor(() => expect(remove).toHaveBeenCalled());
    async function returnFromLogin(hash = '') {
        const returnId = await prepareEditorDraftLogin();
        expect(returnId).toBeTruthy();
        view.unmount();
        window.history.replaceState({}, '', `/form?${EDITOR_DRAFT_RETURN_PARAM}=${returnId}${hash}`);
        initializeEditorDraftLoginReturn();
        mount();
    }
    return {load, save, remove, router, returnFromLogin};
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

function createDerivationOptions() {
    return {
        skipErrorsForElementIds: ['ALL'],
        skipVisibilitiesForElementIds: [],
        skipOverridesForElementIds: [],
        skipValuesForElementIds: [],
    };
}

function createNode(formLayout = createFormLayout()): any {
    return {
        configuration: {
            formLayout: literalAuthoredValue(formLayout),
            formSlug: literalAuthoredValue('test-form'),
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
