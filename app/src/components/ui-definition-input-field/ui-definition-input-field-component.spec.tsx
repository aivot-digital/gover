import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementDisplayContext} from '../../data/element-type/element-child-options';
import {ElementType} from '../../data/element-type/element-type';
import {type UiDefinitionInputFieldElementItem} from '../../models/elements/form/input/ui-definition-input-field-element';
import {UiDefinitionInputFieldComponent} from './ui-definition-input-field-component';

vi.mock('../../providers/confirm-provider', () => ({
    useConfirm: () => vi.fn(),
}));

vi.mock('../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../hooks/use-app-selector', () => ({
    useAppSelector: () => false,
}));

vi.mock('../../hooks/use-not-implemented', () => ({
    useNotImplemented: () => vi.fn(),
}));

vi.mock('../../hooks/use-element-editor-navigation', () => ({
    useElementEditorNavigation: () => ({navigateToElementEditor: vi.fn()}),
}));

vi.mock('../code-editor/code-editor', () => ({
    CodeEditor: () => <div/>,
}));

describe('UiDefinitionInputFieldComponent', () => {
    it('keeps the summary and editor action separate from the field name', () => {
        render(
            <UiDefinitionInputFieldComponent
                label="Ergänzende Oberfläche"
                value={null}
                onChange={vi.fn()}
                hint="Hier kann eine weitere UI-Struktur modelliert werden."
                expectedRootType={ElementType.GroupLayout}
                displayContext={ElementDisplayContext.StaffFacing}
                labelAction={<button type="button">Eingabemodus</button>}
            />,
        );

        const summary = screen.getByRole('group', {name: 'Ergänzende Oberfläche – optional'});
        const action = screen.getByRole('button', {name: 'Bearbeiten'});

        expect(summary).toHaveAccessibleDescription('Hier kann eine weitere UI-Struktur modelliert werden.');
        expect(summary).toHaveTextContent('Keine UI-Definition konfiguriert');
        expect(action).toHaveAttribute('aria-haspopup', 'dialog');
        expect(action).toHaveAttribute('aria-expanded', 'false');
        expect(action).not.toHaveAttribute('aria-controls');
        expect(screen.getByRole('button', {name: 'Eingabemodus'})).toBeInTheDocument();
        expect(action).not.toHaveClass('MuiButton-outlined');
        expect(summary).not.toHaveAccessibleName(/Bearbeiten/);
        expect(getComputedStyle(summary).height).toBe('44px');
    });

    it('keeps a configured one-line summary at the standard control height', () => {
        const value = {
            id: 'root',
            type: ElementType.GroupLayout,
            children: [],
        } as unknown as UiDefinitionInputFieldElementItem;

        render(
            <UiDefinitionInputFieldComponent
                label="Ergänzende Oberfläche"
                value={value}
                onChange={vi.fn()}
                expectedRootType={ElementType.GroupLayout}
                displayContext={ElementDisplayContext.StaffFacing}
            />,
        );

        const summary = screen.getByRole('group', {name: 'Ergänzende Oberfläche – optional'});
        const summaryText = screen.getByText('0 Elemente enthalten');

        expect(getComputedStyle(summary).height).toBe('44px');
        expect(summaryText).toHaveStyle({
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
        });
    });
});
