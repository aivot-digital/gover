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
                    retentionTimeValue: null,
                    retentionTimeUnit: null,
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

    it('blocks incomplete retention times and restores the original values on reset', async () => {
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
        await user.clear(duration);
        await user.type(duration, '3');
        await user.click(screen.getByRole('combobox', {name: /Zeiteinheit/}));
        await user.click(await screen.findByText('Keine Auswahl'));
        expect(screen.getByText('Wählen Sie eine Zeiteinheit für die Aufbewahrungsfrist aus.')).toBeInTheDocument();
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
