import {CaseNumberType} from '../../enums/case-number-type';
import {configureStore} from '@reduxjs/toolkit';
import {act, createRef} from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Provider} from 'react-redux';
import {describe, expect, it, vi} from 'vitest';
import {type ThemeResponseDTO} from '../../../themes/models/theme';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import {RetentionTimeUnit} from '../../enums/retention-time-unit';
import {ProcessStatus} from '../../enums/process-status';
import {
    ProcessSettingsDialogVersionTab,
    type ProcessSettingsDialogVersionTabHandle,
} from './process-settings-dialog-version-tab';

vi.mock('../../../departments/components/department-select-field', () => ({
    DepartmentSelectField: () => null,
}));

vi.mock('../../../../components/rich-text-input-component/rich-text-input-component', () => ({
    RichTextInputComponent: () => null,
}));

describe('ProcessSettingsDialogVersionTab', () => {
    it.each([
        [CaseNumberType.UuidV4, 'Zufällige Kennung (UUID v4)'],
        [CaseNumberType.UuidV7, 'Zeitlich sortierbare Kennung (UUID v7)'],
    ])('defaults to compact identifiers and persists an explicit %s choice', async (type, label) => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 30,
            retentionTimeUnit: RetentionTimeUnit.Days,
        };
        const update = vi
            .spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockImplementation(async (_id, updated) => updated);
        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                />
            </Provider>,
        );
        expect(screen.getByRole('radio', {name: /Kompakte Zufallskennung/})).toBeChecked();
        await user.click(screen.getByRole('radio', {name: label}));
        act(() => ref.current?.save());
        await waitFor(() =>
            expect(update).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    caseNumberType: type,
                    caseNumberTemplate: null,
                }),
            ),
        );
    });

    it('saves the selected theme on the process version', async () => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 30,
            retentionTimeUnit: RetentionTimeUnit.Days,
        };
        const theme = createTheme(11, 'Nordlicht');
        const updatedVersion = {
            ...version,
            themeId: theme.id,
        };
        const onVersionChange = vi.fn();
        const onUnsavedChangesChange = vi.fn();
        const update = vi
            .spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockResolvedValue(updatedVersion);

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[theme]}
                    onVersionChange={onVersionChange}
                    onUnsavedChangesChange={onUnsavedChangesChange}
                />
            </Provider>,
        );

        await user.click(screen.getByRole('combobox', {name: 'Erscheinungsbild – optional'}));
        await user.click(await screen.findByText('Nordlicht'));

        await waitFor(() => {
            expect(onUnsavedChangesChange).toHaveBeenLastCalledWith(true);
        });

        act(() => {
            ref.current?.save();
        });

        await waitFor(() => {
            expect(update).toHaveBeenCalledWith(
                {
                    processDefinitionId: 42,
                    processDefinitionVersion: 7,
                },
                expect.objectContaining({
                    themeId: 11,
                    retentionTimeValue: 30,
                    retentionTimeUnit: RetentionTimeUnit.Days,
                }),
            );
            expect(onVersionChange).toHaveBeenCalledWith(updatedVersion);
        });
    });

    it('saves the retention time and reports unsaved changes', async () => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
        };
        const onUnsavedChangesChange = vi.fn();
        const update = vi
            .spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockImplementation(async (_id, updated) => updated);

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onUnsavedChangesChange={onUnsavedChangesChange}
                />
            </Provider>,
        );

        await user.type(screen.getByRole('textbox', {name: /Aufbewahrungsdauer/}), '12');
        await user.click(screen.getByRole('combobox', {name: /Zeiteinheit/}));
        await user.click(await screen.findByText('Wochen'));

        await waitFor(() => expect(onUnsavedChangesChange).toHaveBeenLastCalledWith(true));
        act(() => ref.current?.save());

        await waitFor(() => expect(update).toHaveBeenCalledWith(
            {processDefinitionId: 42, processDefinitionVersion: 7},
            expect.objectContaining({
                retentionTimeValue: 12,
                retentionTimeUnit: RetentionTimeUnit.Weeks,
            }),
        ));
    });

    it('saves a draft without a retention period but requires both retention fields once one is entered', async () => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
        };
        const onValidationErrorChange = vi.fn();
        const update = vi
            .spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockImplementation(async (_id, updated) => updated);

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>,
        );

        const duration = screen.getByRole('textbox', {name: 'Aufbewahrungsdauer'});
        const unit = screen.getByRole('combobox', {name: 'Zeiteinheit'});
        expect(duration).not.toHaveAttribute('aria-required', 'true');
        expect(unit).not.toHaveAttribute('aria-required', 'true');
        expect(screen.getByText(/Vor der Veröffentlichung muss eine Aufbewahrungsfrist festgelegt sein\./)).toBeInTheDocument();

        await user.type(screen.getByRole('textbox', {name: /Öffentliche Bezeichnung/}), ' neu');
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(false));
        act(() => ref.current?.save());
        await waitFor(() => expect(update).toHaveBeenLastCalledWith(
            expect.anything(),
            expect.objectContaining({
                publicTitle: 'Bauantrag neu',
                retentionTimeValue: null,
                retentionTimeUnit: null,
            }),
        ));
        update.mockClear();

        await user.type(duration, '3');
        expect(screen.getByText('Wählen Sie eine Zeiteinheit für die Aufbewahrungsfrist aus.')).toBeInTheDocument();
        expect(unit).toHaveAttribute('aria-required', 'true');
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(true));
        act(() => ref.current?.save());
        expect(update).not.toHaveBeenCalled();

        act(() => ref.current?.reset());
        expect(screen.getByRole('textbox', {name: 'Aufbewahrungsdauer'})).toHaveValue('');
        expect(screen.queryByText('Wählen Sie eine Zeiteinheit für die Aufbewahrungsfrist aus.')).not.toBeInTheDocument();
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(false));

        await user.type(screen.getByRole('textbox', {name: 'Aufbewahrungsdauer'}), '3');
        await user.click(unit);
        await user.click(await screen.findByRole('option', {name: 'Tage'}));
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(false));
        act(() => ref.current?.save());
        await waitFor(() => expect(update).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                retentionTimeValue: 3,
                retentionTimeUnit: RetentionTimeUnit.Days,
            }),
        ));
    });

    it('blocks invalid retention times and restores the original values on reset', async () => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 30,
            retentionTimeUnit: RetentionTimeUnit.Days,
        };
        const onUnsavedChangesChange = vi.fn();
        const onValidationErrorChange = vi.fn();
        const update = vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'update');

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onUnsavedChangesChange={onUnsavedChangesChange}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>,
        );

        const duration = screen.getByRole('textbox', {name: /Aufbewahrungsdauer/});
        await user.clear(duration);
        expect(screen.getByText('Geben Sie eine Aufbewahrungsdauer an.')).toBeInTheDocument();
        await user.type(duration, '0');
        expect(screen.getByText('Die Aufbewahrungsdauer muss eine positive ganze Zahl sein.')).toBeInTheDocument();
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(true));
        act(() => ref.current?.save());
        expect(update).not.toHaveBeenCalled();

        act(() => ref.current?.reset());
        expect(screen.getByRole('textbox', {name: /Aufbewahrungsdauer/})).toHaveValue('30');
        expect(screen.getByRole('combobox', {name: /Zeiteinheit/})).toHaveTextContent('Tage');
        await waitFor(() => {
            expect(onUnsavedChangesChange).toHaveBeenLastCalledWith(false);
            expect(onValidationErrorChange).toHaveBeenLastCalledWith(false);
        });
    });

    it.each([
        [RetentionTimeUnit.Days, 36_524, 'Tage', '36.524'],
        [RetentionTimeUnit.Weeks, 5_217, 'Wochen', '5.217'],
        [RetentionTimeUnit.Months, 1_200, 'Monate', '1.200'],
        [RetentionTimeUnit.Years, 100, 'Jahre', '100'],
    ])('enforces the retention maximum in %s', async (unit, maximum, label, formattedMaximum) => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 1,
            retentionTimeUnit: unit,
        };
        const update = vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockImplementation(async (_id, updated) => updated);

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                />
            </Provider>,
        );

        const duration = screen.getByRole('textbox', {name: /Aufbewahrungsdauer/});
        await user.clear(duration);
        await user.type(duration, String(maximum + 1));
        expect(screen.getByText(`Die Aufbewahrungsdauer darf maximal ${formattedMaximum} ${label} betragen.`))
            .toBeInTheDocument();
        act(() => ref.current?.save());
        expect(update).not.toHaveBeenCalled();

        await user.clear(duration);
        await user.type(duration, String(maximum));
        act(() => ref.current?.save());
        await waitFor(() => expect(update).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({retentionTimeValue: maximum, retentionTimeUnit: unit}),
        ));
    });

    it('rechecks the maximum when the retention unit changes', async () => {
        const user = userEvent.setup();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 1_200,
            retentionTimeUnit: RetentionTimeUnit.Months,
        };
        const onValidationErrorChange = vi.fn();

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>,
        );

        await user.click(screen.getByRole('combobox', {name: /Zeiteinheit/}));
        await user.click(await screen.findByText('Jahre'));
        expect(screen.getByText('Die Aufbewahrungsdauer darf maximal 100 Jahre betragen.'))
            .toBeInTheDocument();
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(true));

        await user.click(screen.getByRole('combobox', {name: /Zeiteinheit/}));
        await user.click(await screen.findByText('Monate'));
        expect(screen.queryByText('Die Aufbewahrungsdauer darf maximal 100 Jahre betragen.'))
            .not.toBeInTheDocument();
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(false));
    });

    it('saves an optional processing deadline in calendar days and allows removing it', async () => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        // A draft without a retention period must not block saving the processing deadline.
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
        };
        const onUnsavedChangesChange = vi.fn();
        const onValidationErrorChange = vi.fn();
        const update = vi
            .spyOn(ProcessDefinitionVersionApiService.prototype, 'update')
            .mockImplementation(async (_id, updated) => updated);
        const renderTab = (current: typeof version) => (
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={current}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onUnsavedChangesChange={onUnsavedChangesChange}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>
        );
        const {rerender} = render(renderTab(version));

        const deadline = screen.getByRole('textbox', {name: /Bearbeitungsdauer/});
        expect(deadline).toHaveValue('');
        expect(deadline).toHaveAccessibleDescription(/Ohne Angabe gilt für Vorgänge dieser Prozessversion keine Bearbeitungsfrist/);
        await user.type(deadline, '30');
        await waitFor(() => expect(onUnsavedChangesChange).toHaveBeenLastCalledWith(true));
        expect(onValidationErrorChange).toHaveBeenLastCalledWith(false);
        act(() => ref.current?.save());
        await waitFor(() => expect(update).toHaveBeenLastCalledWith(
            {processDefinitionId: 42, processDefinitionVersion: 7},
            expect.objectContaining({instanceDeadlineDays: 30}),
        ));

        // The dialog passes the saved version back in.
        rerender(renderTab({...version, instanceDeadlineDays: 30}));
        await user.clear(screen.getByRole('textbox', {name: /Bearbeitungsdauer/}));
        act(() => ref.current?.save());
        await waitFor(() => expect(update).toHaveBeenLastCalledWith(
            expect.anything(),
            expect.objectContaining({instanceDeadlineDays: null}),
        ));
    });

    it.each(['0', '3653'])('blocks the processing deadline %s outside the task deadline bounds', async (value) => {
        const user = userEvent.setup();
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            retentionTimeValue: 30,
            retentionTimeUnit: RetentionTimeUnit.Days,
            instanceDeadlineDays: 14,
        };
        const onValidationErrorChange = vi.fn();
        const update = vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'update');

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>,
        );

        const deadline = screen.getByRole('textbox', {name: /Bearbeitungsdauer/});
        await user.clear(deadline);
        await user.type(deadline, value);
        expect(screen.getByText('Die Bearbeitungsdauer muss zwischen 1 und 3.652 Tagen liegen.')).toBeInTheDocument();
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(true));
        act(() => ref.current?.save());
        expect(update).not.toHaveBeenCalled();

        act(() => ref.current?.reset());
        expect(screen.getByRole('textbox', {name: /Bearbeitungsdauer/})).toHaveValue('14');
        await waitFor(() => expect(onValidationErrorChange).toHaveBeenLastCalledWith(false));
    });

    it('shows the processing deadline of a published version without allowing changes', () => {
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            status: ProcessStatus.Published,
            instanceDeadlineDays: 21,
        };

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                />
            </Provider>,
        );

        const deadline = screen.getByRole('textbox', {name: /Bearbeitungsdauer/});
        expect(deadline).toHaveValue('21');
        expect(deadline).toBeDisabled();
    });

    it('shows the configured retention time without allowing changes to a published version', () => {
        const ref = createRef<ProcessSettingsDialogVersionTabHandle>();
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            status: ProcessStatus.Published,
            retentionTimeValue: 2,
            retentionTimeUnit: RetentionTimeUnit.Years,
        };
        const update = vi.spyOn(ProcessDefinitionVersionApiService.prototype, 'update');

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    ref={ref}
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                />
            </Provider>,
        );

        expect(screen.getByRole('textbox', {name: /Aufbewahrungsdauer/})).toHaveValue('2');
        expect(screen.getByRole('textbox', {name: /Aufbewahrungsdauer/})).toBeDisabled();
        expect(screen.getByRole('combobox', {name: /Zeiteinheit/})).toHaveTextContent('Jahre');
        expect(screen.getByRole('combobox', {name: /Zeiteinheit/})).toHaveAttribute('aria-disabled', 'true');
        act(() => ref.current?.save());
        expect(update).not.toHaveBeenCalled();
    });

    it('does not show missing retention errors for a published version', () => {
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            processId: 42,
            processVersion: 7,
            publicTitle: 'Bauantrag',
            status: ProcessStatus.Published,
        };
        const onValidationErrorChange = vi.fn();

        render(
            <Provider store={configureStore({reducer: () => ({})})}>
                <ProcessSettingsDialogVersionTab
                    open
                    version={version}
                    departments={[]}
                    themes={[]}
                    onVersionChange={vi.fn()}
                    onValidationErrorChange={onValidationErrorChange}
                />
            </Provider>,
        );

        expect(screen.queryByText('Geben Sie eine Aufbewahrungsdauer an.')).not.toBeInTheDocument();
        expect(screen.queryByText('Wählen Sie eine Zeiteinheit für die Aufbewahrungsfrist aus.')).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Aufbewahrungsdauer'})).toBeDisabled();
        expect(screen.getByRole('combobox', {name: 'Zeiteinheit'})).toHaveAttribute('aria-disabled', 'true');
        expect(onValidationErrorChange).toHaveBeenLastCalledWith(false);
    });
});

function createTheme(id: number, name: string): ThemeResponseDTO {
    return {
        id,
        name,
        primaryColor: '#005f73',
        secondaryColor: '#ee9b00',
        primaryColorDark: null,
        secondaryColorDark: null,
        faviconKey: null,
        logoKey: null,
        logoKeyDark: null,
    };
}
