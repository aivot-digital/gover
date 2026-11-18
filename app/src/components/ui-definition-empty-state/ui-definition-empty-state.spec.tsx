import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {expect, it, vi} from 'vitest';
import {UiDefinitionEmptyState} from './ui-definition-empty-state';

// JSDOM does not run CSS animations, so visibility must come from the static styles.
it.each([
    {target: 'section' as const, title: 'Ersten Abschnitt anlegen', action: 'Abschnitt hinzufügen'},
    {target: 'element' as const, title: 'Erstes Element hinzufügen', action: 'Element hinzufügen'},
])('keeps the $target empty state visible and usable without animations', async ({target, title, action}) => {
    const onAdd = vi.fn();
    render(<UiDefinitionEmptyState target={target} onAdd={onAdd}/>);

    expect(screen.getByRole('heading', {name: title})).toBeVisible();
    const buttons = screen.getAllByRole('button', {name: action});
    for (const button of buttons) {
        expect(button).toBeVisible();
    }
    await userEvent.click(buttons[1]);
    expect(onAdd).toHaveBeenCalledOnce();
});
