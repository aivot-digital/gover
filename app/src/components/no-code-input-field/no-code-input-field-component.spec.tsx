import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeInputFieldComponent} from './no-code-input-field-component';
import {ElementType} from '../../data/element-type/element-type';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {type AnyElement} from '../../models/elements/any-element';

vi.mock('../../hooks/use-api', () => {
    const api = {};
    return {useApi: () => api};
});
vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../services/no-code-api-service', () => ({
    NoCodeApiService: class {
        getNoCodeOperators = vi.fn().mockResolvedValue([]);
    },
}));
vi.mock('../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper', () => ({
    NoCodeEditorWrapper: (props: {editable: boolean; label?: string}) => (
        <div data-testid="no-code-editor" data-editable={props.editable}>
            {props.label}
        </div>
    ),
}));

const rootElement = {
    type: ElementType.FormLayout,
    id: 'root',
    children: [],
} as unknown as AnyElement;

describe('NoCodeInputFieldComponent', () => {
    it('exposes its summary and edit action as separate accessible concepts', async () => {
        const user = userEvent.setup();

        render(
            <NoCodeInputFieldComponent
                rootElement={rootElement}
                label="Freigabebedingung"
                value={null}
                onChange={vi.fn()}
                desiredReturnType={NoCodeDataType.Boolean}
                hint="Legt fest, ob eine Freigabe erforderlich ist."
            />,
        );

        const summary = screen.getByRole('group', {name: 'Freigabebedingung – optional'});
        expect(summary).toHaveTextContent('Kein Ausdruck definiert');
        expect(summary).toHaveAccessibleDescription('Legt fest, ob eine Freigabe erforderlich ist.');
        expect(getComputedStyle(summary).height).toBe('44px');
        expect(getComputedStyle(summary).borderStyle).toBe('dashed');
        expect(getComputedStyle(summary).backgroundColor).toBe('rgba(0, 0, 0, 0)');
        expect(summary.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');

        const editButton = screen.getByRole('button', {name: 'Bearbeiten'});
        expect(editButton.closest('[data-form-field-label-action]')).not.toBeNull();
        expect(editButton).not.toHaveClass('MuiButton-outlined');
        await user.click(editButton);

        const dialog = document.getElementById(editButton.getAttribute('aria-controls')!);
        expect(dialog).toHaveAttribute('role', 'dialog');
        expect(dialog).toHaveAccessibleName('Freigabebedingung');
        expect(screen.getByTestId('no-code-editor')).toHaveAttribute('data-editable', 'true');
    });

    it('renders one shared error and a view action in read-only mode', () => {
        render(
            <NoCodeInputFieldComponent
                rootElement={rootElement}
                label="Freigabebedingung"
                value={null}
                onChange={vi.fn()}
                desiredReturnType={NoCodeDataType.Boolean}
                error="Definieren Sie einen gültigen Ausdruck."
                readOnly
            />,
        );

        expect(screen.getByRole('group', {name: 'Freigabebedingung – optional'}))
            .toHaveAttribute('aria-invalid', 'true');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
        expect(screen.getByRole('button', {name: 'Ansehen'})).toBeInTheDocument();
    });
});
