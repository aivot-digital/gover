import React from 'react';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ProsunaLogo} from './prosuna-logo';

describe('ProsunaLogo', () => {
    it('renders the complete logo in its brand colors by default', () => {
        const {container} = render(<ProsunaLogo title="Prosuna"/>);
        const paths = container.querySelectorAll('path');

        expect(screen.getByRole('img', {name: 'Prosuna'})).toHaveAttribute('viewBox', '0 0 1000 165');
        expect(paths).toHaveLength(8);
        expect(paths[0]).toHaveAttribute('fill', '#FF613A');
        expect(paths[1]).toHaveAttribute('fill', '#733635');
    });

    it('renders the complete logo in a custom monochrome color when requested', () => {
        const {container} = render(
            <ProsunaLogo
                title="Prosuna"
                colorVariant="monochrome"
                color="#351E1C"
            />,
        );
        const svg = screen.getByRole('img', {name: 'Prosuna'});
        const paths = container.querySelectorAll('path');

        expect(svg).toHaveAttribute('color', '#351E1C');
        expect(paths).toHaveLength(8);
        paths.forEach((path) => expect(path).toHaveAttribute('fill', 'currentColor'));
    });

    it('renders only the symbol when requested', () => {
        const {container} = render(
            <ProsunaLogo
                title="Prosuna"
                variant="symbol"
            />,
        );
        const paths = container.querySelectorAll('path');

        expect(screen.getByRole('img', {name: 'Prosuna'})).toHaveAttribute('viewBox', '0 0 202 165');
        expect(paths).toHaveLength(1);
        expect(paths[0]).toHaveAttribute('fill', '#FF613A');
    });

    it('is decorative when it has no accessible name', () => {
        const {container} = render(<ProsunaLogo/>);

        expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    });

    it('forwards its ref to the SVG element', () => {
        const ref = React.createRef<SVGSVGElement>();

        render(<ProsunaLogo ref={ref}/>);

        expect(ref.current?.tagName).toBe('svg');
    });
});
