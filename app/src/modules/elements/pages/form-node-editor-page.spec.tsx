import {createTheme as createMuiTheme, ThemeProvider} from '@mui/material';
import React from 'react';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createMemoryRouter, Link, RouterProvider} from 'react-router-dom';
import {AuthService} from '../../../services/auth-service';
import {type EditorDraft, EditorDraftStorage} from '../../../services/editor-draft-storage';
import {initializeEditorDraftLoginReturn, prepareEditorDraftLogin, EDITOR_DRAFT_RETURN_PARAM, stopEditorDraftForLogout} from '../../../services/editor-draft-login';
import {UsersApiService} from '../../users/users-api-service';
import {type User} from '../../users/models/user';
import {DragAndDropProvider} from '../../../providers/drag-and-drop-provider';
import {useElementTreeInlineEditorContext} from '../../../components/element-tree-2/components/element-tree-inline-editor-context';
import {ElementsApiService} from '../elements-api-service';
import {type FormLayoutElement} from '../../../models/elements/form-layout-element';
import {FormNodeEditorPage} from './form-node-editor-page';
import {ProcessNodeApiService} from '../../process/services/process-node-api-service';
import type {ProcessNodeEntity} from '../../process/entities/process-node-entity';
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
import {SnackbarAction} from '../../../slices/shell-slice';

const StaffAccount = React.createContext<string | undefined>('staff-user');

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
        useParams: () => ({nodeId: (mocks.withRouter ? router.useParams().nodeId : undefined) ?? '1'}),
        useSearchParams: () => [new URLSearchParams()],
    };
});

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => mocks.dispatch,
}));

vi.mock('../../../hooks/use-app-selector', () => ({
    useAppSelector: (selector: (state: unknown) => unknown) => {
        const userId = React.useContext(StaffAccount);
        return selector({
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
                user: userId == null ? undefined : {id: userId},
            },
        });
    },
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

vi.mock('../components/element-derivation-context', () => ({
    ElementDerivationContext: React.forwardRef((
        {element, onEvent, onDerive, scrollContainerRef}: {
            element: FormLayoutElement;
            onEvent: (values: Record<string, unknown>, event: string) => Promise<void>;
            onDerive?: (values: Record<string, unknown>, derivationOptions: unknown, abort?: AbortSignal) => Promise<unknown>;
            scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
        },
        ref,
    ) => {
        mocks.observeScrollContainer(scrollContainerRef);
        mocks.onDerive = onDerive;
        const inlineEditor = useElementTreeInlineEditorContext();
        const firstSection = element.children?.[0];
        React.useImperativeHandle(ref, () => ({
            replaceAuthoredElementValues: mocks.replaceAuthoredElementValues,
        }));

        return (
            <>
                <button type="button" onClick={() => { void onEvent(mocks.submitValues, 'submit'); }}>
                    Testformular absenden
                </button>
                {firstSection && inlineEditor?.editable && (
                    <button type="button" onClick={() => inlineEditor.deleteElement(firstSection)}>Abschnitt löschen</button>
                )}
            </>
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
                <>
                    <input aria-label="Formulartitel" readOnly={!props.editable} value={props.value.publicTitle ?? ''}
                        onChange={(event) => props.onChange({...props.value, publicTitle: event.target.value})}/>
                    <input aria-label="Reitertitel" readOnly={!props.editable} value={props.value.tabTitle ?? ''}
                        onChange={(event) => props.onChange({...props.value, tabTitle: event.target.value})}/>
                </>
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
        vi.spyOn(EditorDraftStorage, 'openGeneration').mockResolvedValue('initial');
        vi.spyOn(EditorDraftStorage, 'isCurrentGeneration').mockResolvedValue(true);
        vi.spyOn(EditorDraftStorage, 'load').mockResolvedValue(null);
        vi.spyOn(EditorDraftStorage, 'remove').mockResolvedValue(undefined);
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
        const {load, save, returnFromLogin} = await renderOpenTextElement();
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

        const storedLoad = load.getMockImplementation()!;
        const gate = Promise.withResolvers<void>();
        load.mockImplementationOnce(async (...args) => {
            await gate.promise;
            return storedLoad(...args);
        });
        await returnFromLogin('#first-name/properties');
        await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
        expect(screen.queryByRole('textbox', {name: /^Titel/})).not.toBeInTheDocument();
        await act(async () => gate.resolve());
        const priorTitle = action === 'Abbrechen' ? 'Bereits übernommen' : 'Vorname';
        expect(save).toHaveBeenLastCalledWith(expect.objectContaining({
            mine: expect.objectContaining({children: [expect.objectContaining({children: [expect.objectContaining({label: priorTitle})]})]}),
            context: expect.objectContaining({mine: expect.objectContaining({label: 'Noch offen'})}),
        }), 'initial');
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

    it.each(['restore', 'discard', 'absent', 'read failure'])('waits for the new form and its recovery before accepting edits (%s)', async (result) => {
        const {router, load, remove, save} = await renderDraftEditor();
        const get = Promise.withResolvers<ProcessNodeEntity>();
        const read = Promise.withResolvers<EditorDraft<FormLayoutElement> | null>();
        const deletion = Promise.withResolvers<void>();
        const stored = {...createFormLayout(), publicTitle: 'Zweites Formular'};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockReturnValueOnce(get.promise);
        load.mockReturnValueOnce(read.promise);
        remove.mockClear();
        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        expect(screen.getByRole('menuitem', {name: 'XDatenfeld-Schema importieren'})).toBeVisible();

        await act(async () => { await router.navigate('/form/2'); });
        expect(screen.queryByRole('menu')).not.toBeInTheDocument();
        expect(screen.queryByDisplayValue('Testformular')).not.toBeInTheDocument();
        await act(async () => get.resolve({...createNode(stored), id: 2}));
        await waitFor(() => expect(load.mock.lastCall?.slice(0, 2)).toEqual(['staff-user', 'process-node/2/form-layout']));
        expect(screen.queryByRole('textbox', {name: 'Formulartitel'})).not.toBeInTheDocument();
        expect(remove).not.toHaveBeenCalled();
        expect(save).not.toHaveBeenCalled();
        if (result === 'restore') {
            const submit = screen.queryByRole('button', {name: 'Testformular absenden'});
            if (submit) await act(async () => { fireEvent.click(submit); });
            expect(FormTriggerApiService.prototype.submitForm).not.toHaveBeenCalled();
            expect(ProcessNodeApiService.prototype.update).not.toHaveBeenCalled();
        }

        if (result === 'read failure') {
            vi.spyOn(console, 'error').mockImplementation(() => {});
            await act(async () => read.reject(new Error('Storage unavailable')));
        } else {
            await act(async () => read.resolve(result === 'absent' ? null : formDraft(2, stored, 'Gesicherter Entwurf')));
        }
        if (result === 'restore' || result === 'discard') {
            const restore = await screen.findByRole('button', {name: 'Änderungen wiederherstellen'});
            expect(screen.queryByRole('textbox', {name: 'Formulartitel', hidden: true})).not.toBeInTheDocument();
            expect(remove).not.toHaveBeenCalled();
            if (result === 'restore') {
                fireEvent.click(restore);
            } else {
                remove.mockReturnValueOnce(deletion.promise);
                fireEvent.click(screen.getByRole('button', {name: 'Änderungen verwerfen'}));
                await waitFor(() => expect(remove).toHaveBeenCalledWith('staff-user', 'process-node/2/form-layout', 'initial'));
                expect(screen.queryByRole('textbox', {name: 'Formulartitel'})).not.toBeInTheDocument();
                await act(async () => deletion.resolve());
            }
        }
        expect(await screen.findByRole('textbox', {name: 'Formulartitel'}))
            .toHaveValue(result === 'restore' ? 'Gesicherter Entwurf' : stored.publicTitle);
        editFormTitle('Neue Eingabe');
        await prepareEditorDraftLogin();
        expect(ProcessNodeApiService.prototype.update).not.toHaveBeenCalled();
        if (result === 'read failure') {
            expect(remove).not.toHaveBeenCalled();
            expect(save).not.toHaveBeenCalled();
            vi.mocked(ProcessNodeApiService.prototype.update).mockImplementationOnce(async (id, updated) => ({
                ...updated, id, ports: [], portResolutionProblems: [], deadlineDefinitions: [],
            }));
            fireEvent.click(formSaveButton());
            await waitFor(() => expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.lastCall?.[1])
                .toMatchObject({configuration: {formLayout: {value: {publicTitle: 'Neue Eingabe'}}}}));
        } else {
            expect(save.mock.lastCall?.[0]).toMatchObject({
                userId: 'staff-user', key: 'process-node/2/form-layout', mine: {publicTitle: 'Neue Eingabe'},
            });
        }
    });

    it('ignores the first visit’s draft after reopening the same form and editing it', async () => {
        const {router, load, save} = await renderDraftEditor();
        const firstRead = Promise.withResolvers<EditorDraft<FormLayoutElement>>();
        load.mockReturnValueOnce(firstRead.promise);
        const firstForm = createFormLayout();
        const secondForm = {...createFormLayout(), publicTitle: 'Zweites Formular'};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockImplementation(async (id) => ({
            ...createNode(id === 1 ? firstForm : secondForm), id,
        }));
        await act(async () => { await router.navigate('/form/2'); });
        await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
        await act(async () => { await router.navigate('/form/1'); });
        await screen.findByDisplayValue(firstForm.publicTitle);
        await act(async () => { await router.navigate('/form/2'); });
        await screen.findByDisplayValue(secondForm.publicTitle);
        editFormTitle('Eingabe beim zweiten Besuch');

        await act(async () => firstRead.resolve(formDraft(2, secondForm, 'Entwurf vom ersten Besuch')));
        expect(screen.queryByRole('button', {name: 'Änderungen wiederherstellen'})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Eingabe beim zweiten Besuch');
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({key: 'process-node/2/form-layout', mine: {publicTitle: 'Eingabe beim zweiten Besuch'}});
    });

    it('checks the account’s own draft before the same form can be edited or autosaved', async () => {
        const {changeAccount, load, remove, save} = await renderDraftEditor();
        const read = Promise.withResolvers<EditorDraft<FormLayoutElement>>();
        load.mockReturnValueOnce(read.promise);
        remove.mockClear();
        const stored = {...createFormLayout(), publicTitle: 'Gespeicherter Stand des zweiten Kontos'};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(createNode(stored));
        changeAccount('second-user');
        await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
        expect(load.mock.lastCall?.slice(0, 2)).toEqual(['second-user', 'process-node/1/form-layout']);
        expect(screen.queryByRole('textbox', {name: 'Formulartitel'})).not.toBeInTheDocument();
        expect(remove).not.toHaveBeenCalled();
        expect(save).not.toHaveBeenCalled();

        await act(async () => read.resolve({...formDraft(1, stored, 'Entwurf des zweiten Kontos'), userId: 'second-user'}));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen wiederherstellen'}));
        expect(await screen.findByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Entwurf des zweiten Kontos');
        editFormTitle('Neue Eingabe');
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({userId: 'second-user', key: 'process-node/1/form-layout', base: stored, mine: {publicTitle: 'Neue Eingabe'}});
    });

    it('starts loading only when the staff account is available', async () => {
        const view = render(<StaffAccount.Provider value={undefined}><FormNodeEditorPage/></StaffAccount.Provider>);
        expect(ProcessNodeApiService.prototype.retrieve).not.toHaveBeenCalled();
        expect(EditorDraftStorage.load).not.toHaveBeenCalled();
        expect(screen.queryByRole('button', {name: 'Speichern'})).not.toBeInTheDocument();
        view.rerender(<StaffAccount.Provider value="staff-user"><FormNodeEditorPage/></StaffAccount.Provider>);
        await waitFor(() => expect(EditorDraftStorage.remove).toHaveBeenCalledWith('staff-user', 'process-node/1/form-layout', 'initial'));
        expect(await screen.findByRole('button', {name: 'Testformular absenden'})).toBeEnabled();
    });

    it('keeps published forms available for viewing without reading or changing their drafts', async () => {
        const {router, load, remove, save} = await renderDraftEditor();
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(), id: 2, processVersion: 3});
        vi.mocked(ProcessDefinitionVersionApiService.prototype.retrieve).mockResolvedValue({
            ...createProcessVersion(), processVersion: 3, status: ProcessStatus.Published,
        });
        remove.mockClear();
        await act(async () => { await router.navigate('/form/2'); });
        expect(await screen.findByRole('textbox', {name: 'Formulartitel'})).toHaveAttribute('readonly');
        expect(formSaveButton()).toBeDisabled();
        expect(load).toHaveBeenCalledTimes(1);
        expect(remove).not.toHaveBeenCalled();
        expect(save).not.toHaveBeenCalled();
    });

    it.each([false, true])('discards the edited form before returning to the process (debounce elapsed: %s)', async (saved) => {
        const {load, remove, save} = await renderDraftEditor();
        const marker = `Discarded form title ${crypto.randomUUID()}`;
        fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: marker}});
        if (saved) {
            await waitFor(() => expect(save).toHaveBeenCalledWith(expect.objectContaining({
                mine: expect.objectContaining({publicTitle: marker}),
            }), 'initial'), {timeout: 3000});
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

    it('offers to restore the backed-up changes after reloading when a lock conflict persists', async () => {
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValue(lockConflict);
        await renderModifiedEditor();
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(), name: 'Anderswo gespeichert', lockVersion: 2});

        fireEvent.click(formSaveButton());

        await waitFor(() => expectDispatchedAction('shell/addSnackbarMessage', expect.objectContaining({
            message: expect.stringContaining('Ihre Änderungen sind in diesem Browser zwischengespeichert'),
            action: SnackbarAction.ReloadPage,
        })));
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(3);
        expect(vi.mocked(EditorDraftStorage.save).mock.lastCall?.[0].mine).toMatchObject({publicTitle: 'Bearbeitet'});
    });

    describe('moving and deleting the same form field', () => {
        type Location = 'source' | 'target' | 'removed';
        function layout(location: Location, title = 'Testformular', sibling = 'Nachbarfeld') {
            const email = {id: 'email', type: ElementType.Text, label: 'E-Mail'};
            return {...createFormLayout([{id: 'step', type: ElementType.Step, title: 'Kontaktdaten', children: [
                {id: 'source', type: ElementType.GroupLayout, label: 'Quellgruppe', children: location === 'source' ? [email] : []},
                {id: 'target', type: ElementType.GroupLayout, label: 'Zielgruppe', children: [
                    {id: 'left', type: ElementType.Text, label: 'Davor'},
                    ...(location === 'target' ? [email] : []),
                    {id: 'right', type: ElementType.Text, label: sibling},
                ]},
            ]}]), publicTitle: title};
        }

        async function editLayout(value: FormLayoutElement) {
            const imported = Promise.withResolvers<FormLayoutElement>();
            vi.mocked(XdfApiService.prototype.xdfTransform).mockReturnValueOnce(imported.promise);
            chooseMoreOption('XDatenfeld-Schema importieren');
            await act(async () => imported.resolve(value));
        }

        async function choosePosition(mine: Location, theirs: Location, choice: 'mine' | 'theirs') {
            const group = within(await screen.findByRole('group', {name: /Abweichende Einstellung: Position im Formular/}));
            const mineOption = group.getByRole('radio', {name: 'Meine Änderung soll behalten werden'});
            const storedOption = group.getByRole('radio', {name: 'Gespeicherter Stand soll übernommen werden'});
            const descriptions = {source: /Quellgruppe/, target: /Verschoben nach.*Zielgruppe/i, removed: /Entfernt/i};
            expect(mineOption).toHaveAccessibleDescription(descriptions[mine]);
            expect(storedOption).toHaveAccessibleDescription(descriptions[theirs]);
            fireEvent.click(choice === 'mine' ? mineOption : storedOption);
        }

        const storedTitle = 'Testformular';
        const editedTitle = 'Eigener Formulartitel';
        const storedSibling = 'Anderswo bearbeitetes Nachbarfeld';
        it.each([
            {mine: 'target', theirs: 'removed', choice: 'mine'},
            {mine: 'target', theirs: 'removed', choice: 'theirs'},
            {mine: 'removed', theirs: 'target', choice: 'mine'},
            {mine: 'removed', theirs: 'target', choice: 'theirs'},
        ] as const)('waits after 409 before saving the exact chosen tree ($mine vs $theirs, $choice)', async ({mine, theirs, choice}) => {
            const local = layout(mine, editedTitle);
            const stored = {...createNode(layout(theirs, storedTitle, storedSibling)), lockVersion: 2};
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValueOnce({...createNode(layout('source')), lockVersion: 1}).mockResolvedValue(stored);
            const update = vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValueOnce(lockConflict)
                .mockImplementation(async (_, node) => ({...stored, ...node, lockVersion: 3}));
            await renderDraftEditor();
            await editLayout(local);
            fireEvent.click(formSaveButton());

            await choosePosition(mine, theirs, choice);
            expect(update.mock.calls.map(([, payload]) => payload)).toEqual([{...createNode(local), lockVersion: 1}]);
            fireEvent.click(screen.getByRole('button', {name: 'Übernehmen und speichern'}));
            await waitFor(() => expect(update).toHaveBeenCalledTimes(2));
            const expected = layout(choice === 'mine' ? mine : theirs, editedTitle, storedSibling);
            expect(update.mock.calls[1][1]).toEqual({...createNode(expected), lockVersion: 2});
        });

        it.each([
            {scenario: 'draft', pending: false, mine: 'target', theirs: 'removed'},
            {scenario: 'interrupted move reverted locally', pending: true, mine: 'source', theirs: 'removed'},
            {scenario: 'interrupted move already stored', pending: true, mine: 'removed', theirs: 'target'},
        ] as const)('recovers the exact layout after login ($scenario)', async ({pending, mine, theirs}) => {
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(layout('source')), lockVersion: 1});
            const {returnFromLogin} = await renderDraftEditor();
            const update = vi.mocked(ProcessNodeApiService.prototype.update);
            const put = pending ? pendingFormSave() : undefined;
            if (put) {
                await editLayout(layout('target'));
                fireEvent.click(formSaveButton());
                await waitFor(() => expect(update).toHaveBeenCalledOnce());
            }
            await editLayout(layout(mine, editedTitle));
            const stored = {...createNode(layout(theirs, storedTitle, storedSibling)), lockVersion: 2};
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(stored);
            update.mockImplementation(async (_, node) => ({...stored, ...node, lockVersion: 3}));
            await returnFromLogin();
            const restore = await screen.findByRole('button', {name: 'Änderungen wiederherstellen'});
            if (theirs === 'target') {
                // The GET confirms the interrupted move. Only the later local deletion needs restoring.
                expect(screen.queryByRole('radio')).not.toBeInTheDocument();
            } else {
                await choosePosition(mine, theirs, 'mine');
            }
            fireEvent.click(restore);
            await waitFor(() => expect(formSaveButton()).toBeEnabled());
            if (put) await act(async () => put.resolve(stored));
            expect(update).toHaveBeenCalledTimes(pending ? 1 : 0);

            fireEvent.click(formSaveButton());
            await waitFor(() => expect(update).toHaveBeenCalledTimes(pending ? 2 : 1));
            expect(update.mock.lastCall?.[1]).toEqual({...createNode(layout(mine, editedTitle, storedSibling)), lockVersion: 2});
        });
    });

    it('flushes only the current edit if login starts before a manual save has passed its generation check', async () => {
        const {save} = await renderDraftEditor();
        const check = Promise.withResolvers<boolean>();
        const verify = vi.mocked(EditorDraftStorage.isCurrentGeneration).mockReturnValueOnce(check.promise);
        editFormTitle('Beim Speicherklick');
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(verify).toHaveBeenCalled());
        editFormTitle('Beim Anmelden');
        expect(await prepareEditorDraftLogin()).toBeTruthy();
        await act(async () => check.resolve(true));
        expect(ProcessNodeApiService.prototype.update).not.toHaveBeenCalled();
        expect(save.mock.lastCall?.[0]).toMatchObject({mine: {publicTitle: 'Beim Anmelden'}});
        expect(save.mock.lastCall?.[0].saveCheckpoint).toBeUndefined();
    });

    it('keeps the open form in memory across logout and enables backups only after this account confirms a new save', async () => {
        const {save} = await renderDraftEditor();
        const update = vi.mocked(ProcessNodeApiService.prototype.update);
        const first = pendingFormSave();
        editFormTitle('Vor der Abmeldung');
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(update).toHaveBeenCalledOnce());
        editFormTitle('Noch im Editor');
        await act(async () => { await stopEditorDraftForLogout(); });
        vi.mocked(EditorDraftStorage.openGeneration).mockResolvedValue('new');
        vi.mocked(EditorDraftStorage.isCurrentGeneration).mockImplementation(async (id) => id === 'new');
        await act(async () => first.resolve(createNode({...createFormLayout(), publicTitle: 'Vor der Abmeldung'})));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Noch im Editor');
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        const self = vi.spyOn(UsersApiService.prototype, 'retrieveSelf').mockResolvedValue({id: 'other'} as User);
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(self).toHaveBeenCalledOnce());
        expect(update).toHaveBeenCalledOnce();
        self.mockResolvedValue({id: 'staff-user'} as User);
        const next = pendingFormSave();
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(update).toHaveBeenCalledTimes(2));
        editFormTitle('Spätere Eingabe');
        expect(await prepareEditorDraftLogin()).toBeUndefined();
        await act(async () => next.resolve({...createNode({...createFormLayout(), publicTitle: 'Noch im Editor'}), lockVersion: 3}));
        expect(await prepareEditorDraftLogin()).toBeTruthy();
        expect(save.mock.lastCall).toEqual([expect.objectContaining({
            base: expect.objectContaining({publicTitle: 'Noch im Editor'}),
            mine: expect.objectContaining({publicTitle: 'Spätere Eingabe'}),
        }), 'new']);
    });

    it.each(['Nächste Fassung', 'Testformular'])('keeps a later title (%s) as an unsaved change on top of the confirmed version', async (later) => {
        const {save} = await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        editFormTitle(later);
        const confirmed = {...createFormLayout(), publicTitle: 'Abgesendet'};
        await act(async () => put.resolve({...createNode(confirmed), lockVersion: 2}));

        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue(later);
        expect(formSaveButton()).toBeEnabled();
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({base: confirmed, baseLockVersion: 2, mine: {publicTitle: later}});
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(2));
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.lastCall?.[1]).toMatchObject({
            lockVersion: 2, configuration: {formLayout: {value: {publicTitle: later}}},
        });
    });

    it('keeps submitted and subsequent edits after an ordinary write failure', async () => {
        const {save} = await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        fireEvent.change(screen.getByRole('textbox', {name: 'Reitertitel'}), {target: {value: 'Spätere Eingabe'}});
        await act(async () => put.reject(new Error('write failed')));
        expect(formSaveButton()).toBeEnabled();
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({
            base: {publicTitle: 'Testformular'},
            mine: {publicTitle: 'Abgesendet', tabTitle: 'Spätere Eingabe'},
        });
    });

    it('writes a later reversion on the confirmed version after losing the first response', async () => {
        const initial = {...createNode(), lockVersion: 1};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(initial);
        await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        editFormTitle('Testformular');
        await act(async () => put.reject({status: 504, message: 'Timeout', details: {reason: 'timeout'}, displayableToUser: false}));
        const committed = {...createNode({...createFormLayout(), publicTitle: 'Abgesendet'}), lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(committed);
        vi.mocked(ProcessNodeApiService.prototype.update).mockResolvedValue({...initial, lockVersion: 3});

        fireEvent.click(formSaveButton());

        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(2));
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.calls[1][1]).toMatchObject({
            lockVersion: 2, configuration: {formLayout: {value: {publicTitle: 'Testformular'}}},
        });
        await waitFor(() => expect(formSaveButton()).toBeDisabled());
    });

    it('keeps a reversion after login if the interrupted PUT commits after the recovery read', async () => {
        const initial = {...createNode(), lockVersion: 1};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(initial);
        const {returnFromLogin} = await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce());
        editFormTitle('Testformular');
        await returnFromLogin();
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen wiederherstellen'}));
        const committed = {...createNode({...createFormLayout(), publicTitle: 'Abgesendet'}), lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValueOnce(initial).mockResolvedValue(committed);
        vi.mocked(ProcessNodeApiService.prototype.update).mockRejectedValueOnce(lockConflict).mockResolvedValue({...initial, lockVersion: 3});
        await act(async () => put.resolve(committed));

        fireEvent.click(formSaveButton());

        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(3));
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.lastCall?.[1]).toMatchObject({
            lockVersion: 2, configuration: {formLayout: {value: {publicTitle: 'Testformular'}}},
        });
        expect(formSaveButton()).toBeDisabled();
    });

    it('retains an open element edit separately from the layout confirmed by the PUT', async () => {
        const {save} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Abgesendetes Feld'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        const put = pendingFormSave();
        fireEvent.click(await screen.findByRole('button', {name: 'Speichern'}));
        fireEvent.click(screen.getByText('Abgesendetes Feld'));
        fireEvent.change(await screen.findByRole('textbox', {name: /^Titel/}), {target: {value: 'Offener Titel'}});
        const submitted = vi.mocked(ProcessNodeApiService.prototype.update).mock.calls[0][1];
        await act(async () => put.resolve({...createNode(), ...submitted, lockVersion: 2}));

        expect(screen.getByRole('textbox', {name: /^Titel/})).toHaveValue('Offener Titel');
        await prepareEditorDraftLogin();
        const draft = save.mock.lastCall?.[0];
        expect(draft).toMatchObject({
            baseLockVersion: 2,
            mine: {children: [{children: [{label: 'Abgesendetes Feld'}]}]},
            context: {base: {label: 'Abgesendetes Feld'}, mine: {label: 'Offener Titel'}},
        });
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        expect(await screen.findByRole('button', {name: 'Speichern'})).toBeDisabled();
    });

    it.each([false, true])('applies an open element whose reference calculation overlaps a form save (edit already stored: %s)', async (alreadyStored) => {
        const {save} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Abgesendetes Feld'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        const put = pendingFormSave();
        fireEvent.click(await screen.findByRole('button', {name: 'Speichern'}));
        fireEvent.click(screen.getByText('Abgesendetes Feld'));
        fireEvent.change(await screen.findByRole('textbox', {name: /^Titel/}), {target: {value: 'Spätere Eingabe'}});

        const calculation = Promise.withResolvers<void>();
        const recalculate = vi.mocked(ElementsApiService.prototype.recalculateReferencedIds);
        recalculate.mockImplementationOnce(async (element) => {
            await calculation.promise;
            return element;
        });
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        const confirmed = createFormLayout([{id: 'section', type: ElementType.Step, title: 'Abschnitt', children: [
            {id: 'first-name', type: ElementType.Text, label: alreadyStored ? 'Spätere Eingabe' : 'Abgesendetes Feld', placeholder: 'Serverhinweis'},
        ]}]);
        await act(async () => put.resolve({...createNode(confirmed), lockVersion: 2}));
        await act(async () => calculation.resolve());
        const saveForm = await screen.findByRole('button', {name: 'Speichern'});
        if (alreadyStored) {
            expect(saveForm).toBeDisabled();
            expect(await prepareEditorDraftLogin()).toBeUndefined();
            return;
        }
        expect(recalculate.mock.lastCall?.[0]).toMatchObject({label: 'Spätere Eingabe', placeholder: 'Serverhinweis'});
        expect(saveForm).toBeEnabled();
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({
            base: confirmed, mine: {children: [{children: [{label: 'Spätere Eingabe', placeholder: 'Serverhinweis'}]}]},
        });
        expect(save.mock.lastCall?.[0].context).toBeUndefined();
    });

    it('retries only the submitted changes and can undo a later edit without undoing a remote change', async () => {
        await renderDraftEditor();
        const put = pendingFormSave();
        const retry = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        editFormTitle('Neuere Eingabe');
        const newer = {...createFormLayout(), tabTitle: 'Anderer Tab'};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(newer), lockVersion: 2});
        await act(async () => put.reject(lockConflict));
        const saved = {...newer, publicTitle: 'Abgesendet'};
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.calls[1][1]).toMatchObject({
            lockVersion: 2, configuration: {formLayout: {value: saved}},
        });
        await act(async () => retry.resolve({...createNode(saved), lockVersion: 3}));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Neuere Eingabe');
        fireEvent.click(screen.getByRole('button', {name: 'Änderung rückgängig machen'}));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Abgesendet');
        expect(screen.getByRole('textbox', {name: 'Reitertitel'})).toHaveValue('Anderer Tab');
        expect(formSaveButton()).toBeDisabled();
    });

    it('discards only the submission in a lock conflict and retains edits to another field', async () => {
        await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        fireEvent.change(screen.getByRole('textbox', {name: 'Reitertitel'}), {target: {value: 'Späterer Reitertitel'}});
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({
            ...createNode({...createFormLayout(), publicTitle: 'Server'}), lockVersion: 2,
        });
        await act(async () => put.reject(lockConflict));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Server'));
        expect(screen.getByRole('textbox', {name: 'Reitertitel'})).toHaveValue('Späterer Reitertitel');
        expect(formSaveButton()).toBeEnabled();
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(1);
    });

    it.each(['committed', 'failed'])('recovers edits made during a PUT after sign-in when the server write %s', async (result) => {
        const {returnFromLogin} = await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce());
        if (result === 'committed') {
            editFormTitle('Testformular');
            vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({
                ...createNode({...createFormLayout(), publicTitle: 'Abgesendet'}), lockVersion: 2,
            });
        } else {
            fireEvent.change(screen.getByRole('textbox', {name: 'Reitertitel'}), {target: {value: 'Spätere Eingabe'}});
        }
        await returnFromLogin();
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen wiederherstellen'}));
        const expectedTitle = result === 'committed' ? 'Testformular' : 'Abgesendet';
        await waitFor(() => expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue(expectedTitle));
        if (result === 'failed') {
            expect(screen.getByRole('textbox', {name: 'Reitertitel'})).toHaveValue('Spätere Eingabe');
        }
        expect(formSaveButton()).toBeEnabled();
        await act(async () => result === 'committed' ? put.resolve(createNode()) : put.reject(new Error('late failure')));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue(expectedTitle);
    });

    it('asks on the next save before reinserting a field edited after the previous save click', async () => {
        await renderOpenTextElement();
        const editAndApply = async (title: string) => {
            fireEvent.change(await screen.findByRole('textbox', {name: /^Titel/}), {target: {value: title}});
            fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
            await screen.findByRole('button', {name: 'Speichern'});
        };
        await editAndApply('Abgesendetes Feld');
        const put = pendingFormSave();
        fireEvent.click(formSaveButton());
        fireEvent.click(screen.getByText('Abgesendetes Feld'));
        await editAndApply('Später bearbeitetes Feld');
        const remote = {...createNode(createFormLayout([
            {id: 'section', type: ElementType.Step, title: 'Abschnitt', children: []},
        ])), lockVersion: 2};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(remote);
        await act(async () => put.reject(lockConflict));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        await waitFor(() => expect(formSaveButton()).toBeEnabled());
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(1);
        fireEvent.click(formSaveButton());
        const confirm = await screen.findByRole('button', {name: 'Übernehmen und speichern'});
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(1);
        fireEvent.click(confirm);
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(2));
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.lastCall?.[1]).toMatchObject({
            lockVersion: 2,
            configuration: {formLayout: {value: {children: [{children: [{id: 'first-name', label: 'Später bearbeitetes Feld'}]}]}}},
        });
    });

    it('asks about the deletion of an open field on sign-in and keeps the explicit choice for its next save', async () => {
        const {returnFromLogin} = await renderOpenTextElement();
        fireEvent.change(screen.getByRole('textbox', {name: /^Titel/}), {target: {value: 'Abgesendetes Feld'}});
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        const put = pendingFormSave();
        fireEvent.click(await screen.findByRole('button', {name: 'Speichern'}));
        fireEvent.click(screen.getByText('Abgesendetes Feld'));
        fireEvent.change(await screen.findByRole('textbox', {name: /^Titel/}), {target: {value: 'Offener Titel'}});
        const remote = createFormLayout([{id: 'section', type: ElementType.Step, title: 'Abschnitt', children: []}]);
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(remote), lockVersion: 2});
        await act(async () => put.reject(lockConflict));
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        await waitFor(() => expect(screen.queryByRole('button', {name: 'Übernehmen und speichern'})).not.toBeInTheDocument());
        await returnFromLogin();
        expect(await screen.findByRole('group', {name: 'Abweichende Einstellung: Gesamtes Element'})).toBeVisible();
        fireEvent.click(screen.getByRole('button', {name: 'Änderungen wiederherstellen'}));
        expect(await screen.findByRole('textbox', {name: /^Titel/})).toHaveValue('Offener Titel');
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        fireEvent.click(await screen.findByRole('button', {name: 'Speichern'}));
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(2));
        expect(vi.mocked(ProcessNodeApiService.prototype.update).mock.lastCall?.[1]).toMatchObject({
            lockVersion: 2, configuration: {formLayout: {value: {children: [{children: [{id: 'first-name', label: 'Offener Titel'}]}]}}},
        });
    });

    it.each(['resolve', 'conflict'])('ignores a late PUT %s after switching to another form', async (outcome) => {
        const {router} = await renderDraftEditor();
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        fireEvent.click(formSaveButton());
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({
            ...createNode({...createFormLayout(), publicTitle: 'Zweites Formular'}), id: 2,
        });
        await act(async () => { await router.navigate('/form/2'); });
        fireEvent.click(await screen.findByRole('button', {name: 'Änderungen verwerfen'}));
        await screen.findByDisplayValue('Zweites Formular');
        const loads = vi.mocked(ProcessNodeApiService.prototype.retrieve).mock.calls.length;
        await act(async () => outcome === 'resolve' ? put.resolve(createNode()) : put.reject(lockConflict));
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Zweites Formular');
        expect(ProcessNodeApiService.prototype.retrieve).toHaveBeenCalledTimes(loads);
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledTimes(1);
    });

    it('exports the confirmed version once and keeps the next unsaved title', async () => {
        await renderDraftEditor();
        const pdf = new Blob(['PDF']);
        const download = vi.spyOn(FormTriggerApiService.prototype, 'downloadPrintablePdf').mockResolvedValue(pdf);
        const put = pendingFormSave();
        editFormTitle('Abgesendet');
        chooseMoreOption('Vordruck exportieren (.pdf)');
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce());
        editFormTitle('Spätere Eingabe');
        await act(async () => put.resolve(createNode({...createFormLayout(), publicTitle: 'Bestätigter Titel'})));
        expect(download).toHaveBeenCalledExactlyOnceWith(1);
        expect(mocks.downloadBlobFile).toHaveBeenCalledExactlyOnceWith('Bestätigter Titel.pdf', pdf);
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Spätere Eingabe');
        expect(formSaveButton()).toBeEnabled();
    });

    it('prepares test attachments using the confirmed layout while leaving later edits available', async () => {
        const fileLayout = createFormLayout([{id: 'upload', type: ElementType.FileUpload}]);
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(createNode(fileLayout));
        await renderDraftEditor();
        const put = pendingFormSave();
        mocks.submitValues = {upload: literalAuthoredValue([{name: 'test.pdf', size: 10, uri: 'blob:removed-field'}])};
        const fetchFile = vi.fn().mockRejectedValue(new Error('Field was removed'));
        vi.stubGlobal('fetch', fetchFile);
        editFormTitle('Abgesendet');
        fireEvent.click(screen.getByRole('button', {name: 'Testformular absenden'}));
        await waitFor(() => expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce());
        editFormTitle('Spätere Eingabe');
        const saved = createNode({...createFormLayout(), publicTitle: 'Abgesendet'});
        saved.configuration.formSlug = literalAuthoredValue('confirmed-slug');
        await act(async () => put.resolve(saved));
        expect(fetchFile).not.toHaveBeenCalled();
        expect(await screen.findByText('Übermittelt: started-process')).toBeVisible();
        expect(FormTriggerApiService.prototype.submitForm).toHaveBeenCalledOnce();
        expect(vi.mocked(FormTriggerApiService.prototype.submitForm).mock.lastCall?.[1]).toBe('confirmed-slug');
        expect(vi.mocked(FormTriggerApiService.prototype.calculateCosts).mock.lastCall?.[1]).toBe('confirmed-slug');
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Spätere Eingabe');
        expect(formSaveButton()).toBeEnabled();
    });

    it('sends only one save while a save is running and lets the export wait for it', async () => {
        await renderModifiedEditor();
        const download = vi.spyOn(FormTriggerApiService.prototype, 'downloadPrintablePdf').mockResolvedValue(new Blob(['PDF']));
        const put = pendingFormSave();

        fireEvent.click(formSaveButton());
        fireEvent.click(formSaveButton());
        await waitFor(() => expect(formSaveButton()).toBeDisabled());
        chooseMoreOption('Vordruck exportieren (.pdf)');
        await waitFor(() => expect(mocks.confirm).toHaveBeenCalledOnce());

        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce();
        await act(async () => put.resolve(createNode({...createFormLayout(), publicTitle: 'Bearbeitet'})));
        expect(download).toHaveBeenCalledExactlyOnceWith(1);
        expect(ProcessNodeApiService.prototype.update).toHaveBeenCalledOnce();
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

        chooseMoreOption('XDatenfeld-Schema importieren');

        await waitFor(() => expectSnackbar('Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
        expectDispatchedAction('shell/setLoadingMessage', {
            blocking: true,
            estimatedTime: 1000,
            message: 'Importiere XDF',
        });
        expectActionTypeDispatched('shell/clearLoadingMessage');
    });

    it.each(['file selection', 'transformation'])('ignores an XDF import left in %s when another form opens', async (stage) => {
        const {router, save} = await renderDraftEditor();
        const file = Promise.withResolvers<string>();
        const transformation = Promise.withResolvers<FormLayoutElement>();
        const transform = vi.mocked(XdfApiService.prototype.xdfTransform);
        if (stage === 'file selection') mocks.uploadTextFile.mockReturnValueOnce(file.promise);
        else transform.mockReturnValueOnce(transformation.promise);
        chooseMoreOption('XDatenfeld-Schema importieren');
        await waitFor(() => expect(stage === 'file selection' ? mocks.uploadTextFile : transform).toHaveBeenCalledOnce());

        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode({...createFormLayout(), publicTitle: 'Zweites Formular'}), id: 2});
        await act(async () => { await router.navigate('/form/2'); });
        await screen.findByDisplayValue('Zweites Formular');
        editFormTitle('Frische Eingabe');
        const currentImport = Promise.withResolvers<FormLayoutElement>();
        if (stage === 'file selection') {
            await act(async () => file.resolve('<xdf/>'));
            expect(transform).not.toHaveBeenCalled();
        } else {
            expectActionTypeDispatched('shell/clearLoadingMessage');
            mocks.dispatch.mockClear();
            transform.mockReturnValueOnce(currentImport.promise);
            chooseMoreOption('XDatenfeld-Schema importieren');
            await waitFor(() => expectDispatchedAction('shell/setLoadingMessage', expect.objectContaining({message: 'Importiere XDF'})));
            await act(async () => transformation.resolve({...createFormLayout(), publicTitle: 'Importiertes Formular'}));
            expectActionTypeNotDispatched('shell/clearLoadingMessage');
        }
        expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Frische Eingabe');
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({key: 'process-node/2/form-layout', mine: {publicTitle: 'Frische Eingabe'}});
        if (stage === 'transformation') {
            await act(async () => currentImport.resolve({...createFormLayout(), publicTitle: 'Import der neuen Ansicht'}));
            expect(screen.getByRole('textbox', {name: 'Formulartitel'})).toHaveValue('Import der neuen Ansicht');
            expectActionTypeDispatched('shell/clearLoadingMessage');
        }
    });

    it('ignores an element deletion confirmed after a different form has opened', async () => {
        const section = {type: ElementType.Step, id: 'section', title: 'Abschnitt', children: []};
        const layout = createFormLayout([section]);
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue(createNode(layout));
        const {router, save} = await renderDraftEditor();
        const confirmation = Promise.withResolvers<boolean>();
        mocks.confirm.mockReturnValueOnce(confirmation.promise);
        fireEvent.click(screen.getByRole('button', {name: 'Abschnitt löschen'}));
        expect(mocks.confirm).toHaveBeenCalledWith(expect.objectContaining({title: 'Element wirklich löschen'}));
        const next = {...layout, publicTitle: 'Zweites Formular'};
        vi.mocked(ProcessNodeApiService.prototype.retrieve).mockResolvedValue({...createNode(next), id: 2});
        await act(async () => { await router.navigate('/form/2'); });
        await screen.findByDisplayValue(next.publicTitle);
        editFormTitle('Neue Eingabe');
        await act(async () => confirmation.resolve(true));
        await prepareEditorDraftLogin();
        expect(save.mock.lastCall?.[0]).toMatchObject({
            key: 'process-node/2/form-layout', mine: {publicTitle: 'Neue Eingabe', children: [section]},
        });
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
    await waitFor(() => expect(EditorDraftStorage.remove).toHaveBeenCalled());
}

async function renderModifiedEditor(): Promise<void> {
    await renderDraftEditor();
    fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: 'Bearbeitet'}});
}

const lockConflict = {
    status: 409,
    message: 'Der Datensatz wurde zwischenzeitlich geändert.',
    details: {reason: 'lock_version_conflict'},
    displayableToUser: true,
};

function pendingFormSave() {
    const pending = Promise.withResolvers<ProcessNodeEntity>();
    vi.mocked(ProcessNodeApiService.prototype.update).mockReturnValueOnce(pending.promise);
    return pending;
}

function editFormTitle(title: string) {
    fireEvent.change(screen.getByRole('textbox', {name: 'Formulartitel'}), {target: {value: title}});
}

function formSaveButton() {
    return screen.getByRole('button', {name: 'Speichern'});
}

function chooseMoreOption(name: string) {
    fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
    fireEvent.click(screen.getByRole('menuitem', {name}));
}

function formDraft(id: number, base: FormLayoutElement, title: string): EditorDraft<FormLayoutElement> {
    return {
        userId: 'staff-user', tabId: 'editor-tab', key: `process-node/${id}/form-layout`, savedAt: Date.now(),
        baseLockVersion: null, base, mine: {...base, publicTitle: title},
    };
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
            {path: '/form/:nodeId?', element: <FormNodeEditorPage/>},
            {path: '/processes/10/versions/1/nodes/1', element: <Link to="/form">Formular öffnen</Link>},
        ], {initialEntries: ['/form']});
        const content = (userId: string) => (
            <StaffAccount.Provider value={userId}>
                <DragAndDropProvider><RouterProvider router={router}/></DragAndDropProvider>
            </StaffAccount.Provider>
        );
        const view = render(content('staff-user'));
        return {view, router, changeAccount: (userId: string) => view.rerender(content(userId))};
    };
    const {view, router, changeAccount} = mount();
    await waitFor(() => expect(remove).toHaveBeenCalled());
    async function returnFromLogin(hash = '') {
        const returnId = await prepareEditorDraftLogin();
        expect(returnId).toBeTruthy();
        view.unmount();
        window.history.replaceState({}, '', `/form?${EDITOR_DRAFT_RETURN_PARAM}=${returnId}${hash}`);
        initializeEditorDraftLoginReturn();
        mount();
    }
    return {load, save, remove, router, returnFromLogin, changeAccount};
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
