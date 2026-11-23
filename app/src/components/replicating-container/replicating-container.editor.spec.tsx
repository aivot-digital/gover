import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ReplicatingContainerEditor} from './replicating-container.editor';

vi.mock('../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

it('keeps new collections unsortable until sorting is allowed', async () => {
    const user = userEvent.setup();
    const onPatch = vi.fn();

    render(
        <ReplicatingContainerEditor
            element={generateElementWithDefaultValues(ElementType.ReplicatingContainer)}
            onPatch={onPatch}
            editable={true}
            hasSummaryLayoutParent={false}
            scope="application"
        />,
    );

    const checkbox = screen.getByRole('checkbox', {name: 'Sortieren der Datensätze erlauben'});
    expect(checkbox).not.toBeChecked();
    expect(checkbox).toHaveAccessibleDescription(/per Drag-and-drop oder über das Verschieben-Symbol/);

    await user.click(checkbox);

    expect(onPatch).toHaveBeenLastCalledWith({sortable: true});
});
