import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AnimatedProsunaLogo} from './animated-prosuna-logo';

const motionPreference = vi.hoisted(() => ({reduced: false}));

vi.mock('motion/react', async () => {
    const motion = await vi.importActual<Record<string, unknown>>('motion/react');

    return {
        ...motion,
        useReducedMotion: () => motionPreference.reduced,
    };
});

describe('AnimatedProsunaLogo', () => {
    beforeEach(() => {
        motionPreference.reduced = false;
    });

    it('uses the outlined paths and keeps them reset while inactive', () => {
        const {container} = render(<AnimatedProsunaLogo active={false} title="Prosuna"/>);
        const paths = container.querySelectorAll('g > path');

        expect(screen.getByRole('img', {name: 'Prosuna'})).toHaveAttribute('viewBox', '0 0 1210 200');
        expect(paths).toHaveLength(8);
        paths.forEach((path) => {
            expect(path).toHaveAttribute('fill-opacity', '0');
            expect(path).toHaveAttribute('stroke-opacity', '0');
        });
    });

    it('draws and fills all paths after it is activated', async () => {
        const {container} = render(<AnimatedProsunaLogo active title="Prosuna"/>);
        const paths = container.querySelectorAll('g > path');

        await waitFor(() => {
            paths.forEach((path) => {
                expect(path).toHaveAttribute('fill-opacity', '1');
                expect(path).toHaveAttribute('stroke-opacity', '1');
            });
        }, {timeout: 3000});
    });

    it('shows all fills immediately when reduced motion is requested', () => {
        motionPreference.reduced = true;
        const {container} = render(<AnimatedProsunaLogo active title="Prosuna"/>);
        const paths = container.querySelectorAll('g > path');

        paths.forEach((path) => {
            expect(path).toHaveAttribute('fill-opacity', '1');
            expect(path).toHaveAttribute('stroke-opacity', '1');
        });
    });
});
