import {configureStore} from '@reduxjs/toolkit';
import {act, createRef} from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Provider} from 'react-redux';
import {describe, expect, it, vi} from 'vitest';
import {type ThemeResponseDTO} from '../../../themes/models/theme';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
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
                expect.objectContaining({themeId: 11}),
            );
            expect(onVersionChange).toHaveBeenCalledWith(updatedVersion);
        });
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
