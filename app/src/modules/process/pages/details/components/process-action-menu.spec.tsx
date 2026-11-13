import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ProcessActionMenu, type ProcessActionMenuItem} from './process-action-menu';

describe('ProcessActionMenu', () => {
    it('forwards row and switch clicks once and receives the checked state from its caller', () => {
        const onToggle = vi.fn();
        const onClose = vi.fn();
        const toggleItem = (checked: boolean): ProcessActionMenuItem => ({
            type: 'toggle',
            label: 'KI-Chat anzeigen',
            icon: null,
            checked,
            onToggle,
        });

        const {rerender} = render(
            <ProcessActionMenu anchorEl={document.body} onClose={onClose} items={[toggleItem(false)]}/>,
        );

        fireEvent.click(screen.getByRole('menuitem', {name: 'KI-Chat anzeigen'}));
        expect(onToggle).toHaveBeenCalledTimes(1);
        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('switch', {name: 'KI-Chat anzeigen'})).not.toBeChecked();

        rerender(<ProcessActionMenu anchorEl={document.body} onClose={onClose} items={[toggleItem(true)]}/>);
        expect(screen.getByRole('switch', {name: 'KI-Chat anzeigen'})).toBeChecked();

        fireEvent.click(screen.getByRole('switch', {name: 'KI-Chat anzeigen'}));
        expect(onToggle).toHaveBeenCalledTimes(2);
        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('menuitem', {name: 'KI-Chat anzeigen'})).toBeInTheDocument();
    });

    it('ignores disabled toggles and closes after a normal action', () => {
        const onToggle = vi.fn();
        const onClick = vi.fn();
        const onClose = vi.fn();

        render(
            <ProcessActionMenu
                anchorEl={document.body}
                onClose={onClose}
                items={[
                    {type: 'toggle', label: 'KI-Chat anzeigen', icon: null, checked: false, disabled: true, onToggle},
                    {label: 'Prozess exportieren', icon: null, onClick},
                ]}
            />,
        );

        fireEvent.click(screen.getByRole('menuitem', {name: 'KI-Chat anzeigen'}));
        expect(screen.getByRole('switch', {name: 'KI-Chat anzeigen'})).toBeDisabled();
        fireEvent.click(screen.getByRole('switch', {name: 'KI-Chat anzeigen'}));
        expect(onToggle).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('menuitem', {name: 'Prozess exportieren'}));
        expect(onClick).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
