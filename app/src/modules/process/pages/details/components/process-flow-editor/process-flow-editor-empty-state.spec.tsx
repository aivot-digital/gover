import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactFlowProvider} from '@xyflow/react';
import {expect, it, vi} from 'vitest';
import {ProcessFlowEditorEmptyState} from './process-flow-editor-empty-state';
import styles from './process-flow-editor.css?inline';

// JSDOM does not run CSS animations, so visibility must come from the static styles.
it('keeps the empty state visible and usable without animations', async () => {
    expect(styles).not.toBe('');
    const onAddTrigger = vi.fn();
    render(
        <ReactFlowProvider>
            <style>{styles}</style>
            <ProcessFlowEditorEmptyState label="Auslöser hinzufügen" onAddTrigger={onAddTrigger} verticalOffset={0}/>
        </ReactFlowProvider>,
    );

    expect(screen.getByRole('heading', {name: 'Ersten Auslöser platzieren'})).toBeVisible();
    const button = screen.getByRole('button', {name: 'Auslöser hinzufügen'});
    expect(button).toBeVisible();
    await userEvent.click(button);
    expect(onAddTrigger).toHaveBeenCalledOnce();
});
