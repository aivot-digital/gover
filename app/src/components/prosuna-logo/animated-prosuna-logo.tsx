import React from 'react';
import {styled} from '@mui/material/styles';
import {type MotionProps, useAnimate, useReducedMotion} from 'motion/react';
import ProsunaLogoMainOutlined from './prosuna-logo-main-outlined.svg?react';
import {type ProsunaLogoProps} from './prosuna-logo';

export type AnimatedProsunaLogoProps = Omit<ProsunaLogoProps, keyof MotionProps | 'style'> & {
    active?: boolean;
    style?: React.CSSProperties;
};

const BRAND_SYMBOL_COLOR = '#FF613A';
const BRAND_WORDMARK_COLOR = '#733635';
const LOGO_PATH_SELECTOR = 'g > path';
const LOGO_SYMBOL_SELECTOR = 'g > path:first-of-type';
const LOGO_WORDMARK_SELECTOR = 'g > path:not(:first-of-type)';
const LOGO_OPEN_DELAY_SECONDS = 0.38;
const LOGO_WORDMARK_DELAY_SECONDS = 0.5;
const LOGO_PATH_STAGGER_SECONDS = 0.055;
const LOGO_FILL_DELAY_SECONDS = 1.5;

const StyledOutlinedLogo = styled(ProsunaLogoMainOutlined)({
    'display': 'block',
    'overflow': 'visible',
    '&[data-color-variant="brand"] path:first-of-type': {
        fill: `${BRAND_SYMBOL_COLOR} !important`,
        stroke: `${BRAND_SYMBOL_COLOR} !important`,
    },
    '&[data-color-variant="brand"] path:not(:first-of-type)': {
        fill: `${BRAND_WORDMARK_COLOR} !important`,
        stroke: `${BRAND_WORDMARK_COLOR} !important`,
    },
    '&[data-color-variant="monochrome"] path': {
        fill: 'currentColor !important',
        stroke: 'currentColor !important',
    },
    '&[data-logo-variant="symbol"] path:not(:first-of-type)': {
        display: 'none',
    },
});

function getPathDelay(startDelay: number): (index: number) => number {
    return (index) => startDelay + index * LOGO_PATH_STAGGER_SECONDS;
}

export function AnimatedProsunaLogo({
    active = true,
    variant = 'full',
    colorVariant = 'brand',
    title,
    role,
    style,
    ...svgProps
}: AnimatedProsunaLogoProps): React.ReactElement {
    const shouldReduceMotion = useReducedMotion() === true;
    const [
        scope,
        animate,
    ] = useAnimate<SVGSVGElement>();
    const symbolOnly = variant === 'symbol';
    const hasAccessibleName = title != null ||
        svgProps['aria-label'] != null ||
        svgProps['aria-labelledby'] != null;

    React.useLayoutEffect(() => {
        const logo = scope.current;

        if (logo == null) {
            return;
        }

        const paths = logo.querySelectorAll<SVGPathElement>(LOGO_PATH_SELECTOR);

        paths.forEach((path) => {
            path.style.removeProperty('fill-opacity');
            path.style.removeProperty('stroke-opacity');
            path.setAttribute('fill-opacity', shouldReduceMotion ? '1' : '0');
            path.setAttribute('stroke-opacity', shouldReduceMotion ? '1' : '0');
        });
        logo.style.opacity = '1';

        if (shouldReduceMotion || !active) {
            return;
        }

        const animations = [
            animate(LOGO_SYMBOL_SELECTOR, {
                pathLength: [
                    0,
                    1,
                ],
            }, {
                delay: LOGO_OPEN_DELAY_SECONDS,
                duration: 1.05,
                ease: [
                    0.4,
                    0,
                    0.2,
                    1,
                ],
            }),
            animate(LOGO_WORDMARK_SELECTOR, {
                pathLength: [
                    0,
                    1,
                ],
            }, {
                delay: getPathDelay(LOGO_WORDMARK_DELAY_SECONDS),
                duration: 0.52,
                ease: [
                    0.4,
                    0,
                    0.2,
                    1,
                ],
            }),
            animate(LOGO_SYMBOL_SELECTOR, {
                strokeOpacity: [
                    0,
                    1,
                    1,
                ],
            }, {
                delay: LOGO_OPEN_DELAY_SECONDS,
                duration: 1.05,
                times: [
                    0,
                    0.05,
                    1,
                ],
                ease: 'easeInOut',
            }),
            animate(LOGO_WORDMARK_SELECTOR, {
                strokeOpacity: [
                    0,
                    1,
                    1,
                ],
            }, {
                delay: getPathDelay(LOGO_WORDMARK_DELAY_SECONDS),
                duration: 0.86,
                times: [
                    0,
                    0.08,
                    1,
                ],
                ease: 'easeInOut',
            }),
            animate(LOGO_PATH_SELECTOR, {
                fillOpacity: [
                    0,
                    1,
                ],
            }, {
                delay: LOGO_FILL_DELAY_SECONDS,
                duration: 0.58,
                ease: [
                    0.4,
                    0,
                    0.2,
                    1,
                ],
            }),
        ];

        return () => {
            animations.forEach((animation) => {
                animation.stop();
            });
        };
    }, [
        active,
        animate,
        scope,
        shouldReduceMotion,
    ]);

    return (
        <StyledOutlinedLogo
            ref={scope}
            width={symbolOnly ? 244 : 1210}
            height={200}
            viewBox={symbolOnly ? '0 0 244 200' : '0 0 1210 200'}
            role={role ?? (hasAccessibleName ? 'img' : undefined)}
            aria-label={svgProps['aria-label'] ?? title}
            aria-hidden={hasAccessibleName ? undefined : true}
            focusable="false"
            data-color-variant={colorVariant}
            data-logo-variant={variant}
            style={{
                opacity: 0,
                ...style,
            }}
            {...svgProps}
        />
    );
}
