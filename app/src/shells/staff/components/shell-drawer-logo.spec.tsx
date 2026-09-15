import * as React from 'react';
import {fireEvent, render, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import ShellDrawerLogo from './shell-drawer-logo';

const motionPreference = vi.hoisted(() => ({reduced: true}));

vi.mock('motion/react', async () => {
    const motion = await vi.importActual<Record<string, unknown>>('motion/react');

    return {
        ...motion,
        useReducedMotion: () => motionPreference.reduced,
    };
});

describe('ShellDrawerLogo', () => {
    beforeEach(() => {
        motionPreference.reduced = true;
    });

    it('renders the complete logo in its expanded end state', () => {
        const {container} = render(<ShellDrawerLogo hoverBackgroundColor="#121212"/>);
        const logo = container.querySelector('svg');
        const symbol = container.querySelector('[data-logo-symbol="fill"]');
        const outline = container.querySelector('[data-logo-symbol="outline"]');
        const hoverBackground = container.querySelector('[data-logo-hover-layer="background"]');
        const hoverRestore = container.querySelector('[data-logo-hover-layer="restore"]');
        const wordmark = container.querySelector('[data-logo-wordmark]');
        const revealRoutes = container.querySelectorAll('[data-logo-reveal-route]');
        const hoverBackgroundRoutes = container.querySelectorAll('[data-logo-hover-mask="background"]');
        const hoverRestoreRoutes = container.querySelectorAll('[data-logo-hover-mask="restore"]');

        expect(logo).toHaveStyle({width: '139px'});
        expect(logo).toHaveAttribute('viewBox', '0 0 139 40');
        expect(revealRoutes).toHaveLength(2);
        expect(hoverBackgroundRoutes).toHaveLength(2);
        expect(hoverRestoreRoutes).toHaveLength(2);
        revealRoutes.forEach((route) => {
            expect(route).toHaveAttribute('pathLength', '1');
            expect(route).toHaveAttribute('stroke-dasharray', '1 1');
            expect(route).toHaveAttribute('stroke-dashoffset', '0');
        });
        expect(symbol).toHaveAttribute('fill', 'currentColor');
        expect(symbol).not.toHaveAttribute('stroke');
        expect(symbol).not.toHaveAttribute('stroke-width');
        expect(symbol).not.toHaveAttribute('mask');
        expect(outline).toHaveStyle({opacity: '1'});
        expect(outline).toHaveAttribute('pathLength', '1');
        expect(outline).toHaveAttribute('stroke-dasharray', '1 1');
        expect(outline).toHaveAttribute('stroke-dashoffset', '0');
        expect(hoverBackground).toHaveAttribute('fill', '#121212');
        expect(hoverBackground).not.toHaveAttribute('stroke');
        expect(hoverBackground).toHaveStyle({opacity: '0'});
        expect(hoverBackground).toHaveStyle({visibility: 'hidden'});
        expect(hoverRestore).toHaveAttribute('fill', 'currentColor');
        expect(hoverRestore).not.toHaveAttribute('stroke');
        expect(hoverRestore).toHaveStyle({opacity: '0'});
        expect(hoverRestore).toHaveStyle({visibility: 'hidden'});
        expect(wordmark).toHaveStyle({opacity: '0.8'});
    });

    it('keeps the hidden wordmark mounted in its minimized end state', () => {
        const {container} = render(<ShellDrawerLogo minimize/>);
        const logo = container.querySelector('svg');
        const wordmark = container.querySelector('[data-logo-wordmark]');

        expect(logo).toHaveStyle({width: '33px'});
        expect(logo).toHaveAttribute('viewBox', '0 0 33 40');
        expect(wordmark).toHaveStyle({opacity: '0'});
    });

    it('starts in the intro end state and animates only when hovered', async () => {
        motionPreference.reduced = false;
        const {container} = render(<ShellDrawerLogo hoverBackgroundColor="#121212"/>);
        const logo = container.querySelector('svg');
        const symbol = container.querySelector<SVGPathElement>('[data-logo-symbol="fill"]');
        const outline = container.querySelector<SVGPathElement>('[data-logo-symbol="outline"]');
        const hoverBackground = container.querySelector<SVGPathElement>('[data-logo-hover-layer="background"]');
        const hoverRestore = container.querySelector<SVGPathElement>('[data-logo-hover-layer="restore"]');
        const hoverBackgroundRoutes = container.querySelectorAll<SVGPathElement>('[data-logo-hover-mask="background"]');
        const hoverRestoreRoutes = container.querySelectorAll<SVGPathElement>('[data-logo-hover-mask="restore"]');

        expect(symbol?.getAttribute('mask')).toMatch(/^url\(#shell-logo-reveal-/);
        expect(outline).toHaveStyle({opacity: '1'});
        expect(hoverBackground).toHaveStyle({
            opacity: '0',
            visibility: 'hidden',
        });
        expect(hoverRestore).toHaveStyle({
            opacity: '0',
            visibility: 'hidden',
        });

        expect(logo).not.toBeNull();
        if (logo == null) {
            throw new Error('Expected the logo SVG to be rendered.');
        }

        fireEvent.pointerEnter(logo, {pointerType: 'mouse'});

        await waitFor(() => {
            expect(Number(hoverBackground?.style.opacity)).toBeGreaterThan(0.2);
            expect(Number(hoverRestore?.style.opacity)).toBeGreaterThan(0.2);
            expect(hoverBackground).toHaveStyle({visibility: 'visible'});
            expect(hoverRestore).toHaveStyle({visibility: 'visible'});
        }, {timeout: 500});

        await waitFor(() => {
            expect(hoverBackground).toHaveStyle({
                opacity: '0',
                visibility: 'hidden',
            });
            expect(hoverRestore).toHaveStyle({
                opacity: '0',
                visibility: 'hidden',
            });
            hoverBackgroundRoutes.forEach((route) => {
                expect(route).toHaveAttribute('stroke-dasharray', '0 1');
            });
            hoverRestoreRoutes.forEach((route) => {
                expect(route).toHaveAttribute('stroke-dasharray', '0 1');
            });
        }, {timeout: 1500});
    });

    it('stops a running hover animation when reduced motion is enabled', async () => {
        motionPreference.reduced = false;
        const {container, rerender} = render(<ShellDrawerLogo hoverBackgroundColor="#121212"/>);
        const logo = container.querySelector('svg');
        const hoverBackground = container.querySelector<SVGPathElement>('[data-logo-hover-layer="background"]');
        const hoverRestore = container.querySelector<SVGPathElement>('[data-logo-hover-layer="restore"]');

        expect(logo).not.toBeNull();
        if (logo == null) {
            throw new Error('Expected the logo SVG to be rendered.');
        }

        fireEvent.pointerEnter(logo, {pointerType: 'mouse'});

        await waitFor(() => {
            expect(Number(hoverBackground?.style.opacity)).toBeGreaterThan(0.2);
            expect(Number(hoverRestore?.style.opacity)).toBeGreaterThan(0.2);
        }, {timeout: 500});

        motionPreference.reduced = true;
        rerender(<ShellDrawerLogo hoverBackgroundColor="#121212"/>);

        expect(hoverBackground).toHaveStyle({
            opacity: '0',
            visibility: 'hidden',
        });
        expect(hoverRestore).toHaveStyle({
            opacity: '0',
            visibility: 'hidden',
        });
    });
});
