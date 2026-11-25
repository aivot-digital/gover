import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {NoCodePill} from './no-code-pill';

describe('NoCodePill', () => {
    it('names the pill by its label and data type and labels the dismiss button separately', () => {
        const onClick = vi.fn();
        const onDismiss = vi.fn();

        render(
            <NoCodePill
                dataType={NoCodeDataType.Boolean}
                label="Ist Gleich"
                dismissLabel="Funktion „Ist Gleich“ löschen"
                onClick={onClick}
                onDismiss={onDismiss}
            />,
        );

        const pill = screen.getByRole('button', {name: 'Ist Gleich, Wahrheitswert'});
        expect(pill).toHaveTextContent('Ist Gleich');
        fireEvent.click(pill);
        expect(onClick).toHaveBeenCalledTimes(1);
        expect(onDismiss).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Funktion „Ist Gleich“ löschen'}));
        expect(onDismiss).toHaveBeenCalledTimes(1);
    });

    it('describes the pill by its error', () => {
        render(
            <NoCodePill
                dataType={NoCodeDataType.String}
                label="Vorname"
                dismissLabel="Verweis auf Formularelement „Vorname“ löschen"
                error="Das Formularelement existiert nicht mehr."
                onClick={vi.fn()}
                onDismiss={vi.fn()}
            />,
        );

        expect(screen.getByRole('button', {name: 'Vorname, Textwert'}))
            .toHaveAccessibleDescription('Das Formularelement existiert nicht mehr.');
    });
});
