import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeDataType} from '../../../../data/no-code-data-type';
import {ElementType} from '../../../../data/element-type/element-type';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import {NoCodeEditorWrapper} from './no-code-editor-wrapper';

vi.mock('../../../../hooks/use-api', () => ({useApi: () => ({get: async () => []})}));
vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const root = generateElementWithDefaultValues(ElementType.FormLayout);

describe('NoCodeEditorWrapper', () => {
    it('forwards the editability boundary to the No-Code editor without blocking descriptions', async () => {
        const onChange = vi.fn();
        render(<NoCodeEditorWrapper
            parents={[root]}
            noCode={{
                type: 'NoCodeStaticValue',
                value: 'Referenz',
            }}
            onChange={onChange}
            editable={false}
            desiredReturnType={NoCodeDataType.String}
            label="Ausdruck"
        />);
        const tree = await screen.findByRole('tree', {name: 'Ausdruck'});
        expect(tree).toHaveAttribute('aria-readonly', 'true');
        const item = screen.getByRole('treeitem', {name: 'Ausdruck: Referenz, Textwert'});
        fireEvent.keyDown(item, {key: 'Delete'});
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.keyDown(item, {key: 'Enter'});
        expect(await screen.findByRole('dialog')).toHaveTextContent('Fester Wert');
    });
});

it.each([null, 'old_editor', 'pill-no-code', 'no-code-editor'])('uses the new editor without relying on the legacy switch %s', async (flag) => {
    localStorage.clear();
    if (flag) localStorage.setItem(flag, 'true');
    render(<NoCodeEditorWrapper parents={[root]}
                                noCode={null}
                                onChange={vi.fn()}
                                editable
                                desiredReturnType={NoCodeDataType.Boolean}
                                label="Bedingung"/>);
    expect(await screen.findByRole('button', {name: 'Ausdruck hinzufügen'})).toBeInTheDocument();
    expect(screen.getByRole('tree', {name: 'Bedingung'})).toBeInTheDocument();
    localStorage.clear();
});
