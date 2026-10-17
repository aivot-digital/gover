/* eslint-disable max-len -- SVG path data is intentionally kept intact. */
import * as React from 'react';
import {motion, type SVGMotionProps, useAnimate, useReducedMotion} from 'motion/react';
import {
    SHELL_DRAWER_REDUCED_MOTION_TRANSITION,
    SHELL_DRAWER_TRANSITION,
} from './shell-drawer-motion';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

interface ShellDrawerLogoProps extends SVGMotionProps<SVGSVGElement> {
    minimize?: boolean;
    hoverBackgroundColor?: string;
}

const SYMBOL_HOVER_BACKGROUND_MASK_SELECTOR = '[data-logo-hover-mask="background"]';
const SYMBOL_HOVER_RESTORE_MASK_SELECTOR = '[data-logo-hover-mask="restore"]';
const SYMBOL_HOVER_LAYER_SELECTOR = '[data-logo-hover-layer]';
const SYMBOL_REVEAL_STROKE_WIDTH = 5.2;
// Two synchronized masks first paint the drawer surface across the symbol and then restore its foreground color.
const SYMBOL_PATH = 'M8.27832 6.59961H25.2305C25.4008 6.59961 25.5601 6.67696 25.665 6.80566L25.707 6.86426L27.416 9.62402C27.5045 9.76657 27.5226 9.94114 27.4678 10.0986L27.4404 10.165L20.3213 24.377C20.227 24.5649 20.0336 24.6845 19.8213 24.6846H10.5068L10.3408 25.0166L8.4082 28.873L7.97363 29.7422H23.3115L23.4775 29.4111L29.2734 17.8398L29.709 16.9707H27.5342L28.8643 14.3135H30.0234C30.2181 14.3135 30.3978 14.4146 30.499 14.5781L32.208 17.3369V17.3379C32.3091 17.5006 32.3188 17.7047 32.2324 17.8779L25.1143 32.0898C25.02 32.278 24.8259 32.3984 24.6133 32.3984H7.66016C7.46537 32.3984 7.28483 32.2974 7.18359 32.1338L5.47461 29.375V29.374C5.37356 29.2113 5.36389 29.0072 5.4502 28.834L12.5693 14.6221C12.6636 14.4339 12.8569 14.3136 13.0693 14.3135H22.3838L22.5498 13.9824L24.4824 10.125L24.917 9.25684H9.5791L9.41309 9.58789L3.61816 21.1582L3.18262 22.0273H5.35742L4.02734 24.6846H2.86816C2.69775 24.6846 2.53851 24.6073 2.43359 24.4785L2.39258 24.4199L0.683594 21.6602H0.682617C0.594118 21.5177 0.576088 21.343 0.630859 21.1855L0.65918 21.1191L7.77734 6.9082C7.87162 6.72002 8.06576 6.59961 8.27832 6.59961ZM14.2051 17.3018L12.2725 21.1592L11.8379 22.0273H18.5195L18.6855 21.6963L20.6182 17.8398L21.0527 16.9707H14.3711L14.2051 17.3018Z';
const SYMBOL_REVEAL_ROUTES = [
    'M4.69 23.36L1.98 21.55L8.65 7.93H25.75L26.4 9.6L22.35 16.25L22.15 17.45L18.85 23.36H14.75',
    'M28.2 15.64L30.95 17.45L24.2 31.07H7.8L10.55 23.36L13.8 16.45L14.4 15.64H17.8',
] as const;

const ShellDrawerLogo: React.FC<ShellDrawerLogoProps> = ({
    minimize = false,
    hoverBackgroundColor = 'transparent',
    style,
    onHoverStart,
    ...props
}) => {
    const shouldReduceMotion = useReducedMotion() === true;
    const maskId = useNormalizedReactId();
    const revealMaskId = `shell-logo-reveal-${maskId}`;
    const hoverBackgroundMaskId = `shell-logo-hover-background-${maskId}`;
    const hoverRestoreMaskId = `shell-logo-hover-restore-${maskId}`;
    const [
        scope,
        animate,
    ] = useAnimate<SVGSVGElement>();
    const hoverAnimationIdRef = React.useRef(0);
    const stopHoverAnimationRef = React.useRef<VoidFunction>(() => undefined);
    const transition = shouldReduceMotion ?
        SHELL_DRAWER_REDUCED_MOTION_TRANSITION :
        SHELL_DRAWER_TRANSITION;

    const playHoverAnimation = React.useCallback((): void => {
        stopHoverAnimationRef.current();

        if (shouldReduceMotion) {
            return;
        }

        const hoverLayers = scope.current?.querySelectorAll<SVGPathElement>(SYMBOL_HOVER_LAYER_SELECTOR);
        const hoverAnimationId = hoverAnimationIdRef.current + 1;
        hoverAnimationIdRef.current = hoverAnimationId;

        hoverLayers?.forEach((layer) => {
            layer.style.visibility = 'visible';
        });

        const hoverAnimation = animate([
            [
                SYMBOL_HOVER_LAYER_SELECTOR,
                {opacity: 0},
                {
                    duration: 0,
                },
            ],
            [
                SYMBOL_HOVER_BACKGROUND_MASK_SELECTOR,
                {pathLength: 0},
                {
                    at: '<',
                    duration: 0,
                },
            ],
            [
                SYMBOL_HOVER_RESTORE_MASK_SELECTOR,
                {pathLength: 0},
                {
                    at: '<',
                    duration: 0,
                },
            ],
            [
                SYMBOL_HOVER_LAYER_SELECTOR,
                {
                    opacity: [
                        1,
                        1,
                        0,
                    ],
                },
                {
                    at: '<',
                    duration: 1.18,
                    times: [
                        0,
                        0.92,
                        1,
                    ],
                },
            ],
            [
                SYMBOL_HOVER_BACKGROUND_MASK_SELECTOR,
                {pathLength: 1},
                {
                    at: '<',
                    duration: 0.92,
                    ease: [
                        0.4,
                        0,
                        0.2,
                        1,
                    ],
                },
            ],
            [
                SYMBOL_HOVER_RESTORE_MASK_SELECTOR,
                {pathLength: 1},
                {
                    at: 0.14,
                    duration: 0.92,
                    ease: [
                        0.4,
                        0,
                        0.2,
                        1,
                    ],
                },
            ],
            [
                SYMBOL_HOVER_LAYER_SELECTOR,
                {opacity: 0},
                {
                    at: 1.18,
                    duration: 0,
                },
            ],
            [
                SYMBOL_HOVER_BACKGROUND_MASK_SELECTOR,
                {pathLength: 0},
                {
                    at: 1.19,
                    duration: 0,
                },
            ],
            [
                SYMBOL_HOVER_RESTORE_MASK_SELECTOR,
                {pathLength: 0},
                {
                    at: '<',
                    duration: 0,
                },
            ],
        ]);

        void hoverAnimation.then(() => {
            if (hoverAnimationIdRef.current !== hoverAnimationId) {
                return;
            }

            hoverLayers?.forEach((layer) => {
                layer.style.visibility = 'hidden';
            });
        });

        stopHoverAnimationRef.current = () => {
            if (hoverAnimationIdRef.current === hoverAnimationId) {
                hoverAnimationIdRef.current += 1;
            }

            hoverAnimation.stop();
            hoverLayers?.forEach((layer) => {
                layer.style.opacity = '0';
                layer.style.visibility = 'hidden';
            });
        };
    }, [
        animate,
        shouldReduceMotion,
    ]);

    React.useEffect(() => () => {
        stopHoverAnimationRef.current();
    }, []);

    React.useLayoutEffect(() => {
        if (shouldReduceMotion) {
            stopHoverAnimationRef.current();
        }
    }, [shouldReduceMotion]);

    return (
        <motion.svg
            ref={scope}
            xmlns="http://www.w3.org/2000/svg"
            initial={false}
            animate={{
                width: minimize ? 33 : 139,
                viewBox: minimize ? '0 0 33 40' : '0 0 139 40',
                x: minimize ? 3.5 : 0,
            }}
            transition={transition}
            height="40"
            fill="none"
            style={{
                display: 'block',
                ...style,
            }}
            onHoverStart={(event, info) => {
                onHoverStart?.(event, info);
                playHoverAnimation();
            }}
            {...props}
        >
            <defs>
                <mask
                    id={revealMaskId}
                    maskUnits="userSpaceOnUse"
                    x="-1"
                    y="5"
                    width="35"
                    height="29"
                    style={{maskType: 'alpha'}}
                >
                    {SYMBOL_REVEAL_ROUTES.map((route) => (
                        <motion.path
                            key={route}
                            data-logo-reveal-route="true"
                            d={route}
                            fill="none"
                            stroke="white"
                            strokeWidth={SYMBOL_REVEAL_STROKE_WIDTH}
                            strokeLinecap="square"
                            strokeLinejoin="round"
                            pathLength="1"
                            strokeDasharray="1 1"
                            strokeDashoffset="0"
                        />
                    ))}
                </mask>

                <mask
                    id={hoverBackgroundMaskId}
                    maskUnits="userSpaceOnUse"
                    x="-1"
                    y="5"
                    width="35"
                    height="29"
                    style={{maskType: 'alpha'}}
                >
                    {SYMBOL_REVEAL_ROUTES.map((route) => (
                        <motion.path
                            key={route}
                            data-logo-hover-mask="background"
                            d={route}
                            fill="none"
                            stroke="white"
                            strokeWidth={SYMBOL_REVEAL_STROKE_WIDTH}
                            strokeLinecap="square"
                            strokeLinejoin="round"
                            style={{pathLength: 0}}
                        />
                    ))}
                </mask>

                <mask
                    id={hoverRestoreMaskId}
                    maskUnits="userSpaceOnUse"
                    x="-1"
                    y="5"
                    width="35"
                    height="29"
                    style={{maskType: 'alpha'}}
                >
                    {SYMBOL_REVEAL_ROUTES.map((route) => (
                        <motion.path
                            key={route}
                            data-logo-hover-mask="restore"
                            d={route}
                            fill="none"
                            stroke="white"
                            strokeWidth={SYMBOL_REVEAL_STROKE_WIDTH}
                            strokeLinecap="square"
                            strokeLinejoin="round"
                            style={{pathLength: 0}}
                        />
                    ))}
                </mask>
            </defs>

            <motion.path
                data-logo-symbol="fill"
                fill="currentColor"
                mask={shouldReduceMotion ? undefined : `url(#${revealMaskId})`}
                d={SYMBOL_PATH}
            />

            <motion.path
                aria-hidden="true"
                data-logo-hover-layer="background"
                fill={hoverBackgroundColor}
                mask={`url(#${hoverBackgroundMaskId})`}
                style={{
                    opacity: 0,
                    visibility: 'hidden',
                }}
                d={SYMBOL_PATH}
            />

            <motion.path
                aria-hidden="true"
                data-logo-hover-layer="restore"
                fill="currentColor"
                mask={`url(#${hoverRestoreMaskId})`}
                style={{
                    opacity: 0,
                    visibility: 'hidden',
                }}
                d={SYMBOL_PATH}
            />

            <motion.path
                aria-hidden="true"
                data-logo-symbol="outline"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.2"
                pathLength="1"
                strokeDasharray="1 1"
                strokeDashoffset="0"
                style={{opacity: 1}}
                d={SYMBOL_PATH}
            />

            <motion.path
                data-logo-wordmark="true"
                fill="currentColor"
                initial={false}
                animate={{
                    opacity: minimize ? 0 : 0.8,
                    x: minimize ? -6 : 0,
                }}
                transition={transition}
                d="M48.357 10H41v19.544h3.164v-7.291h4.37c1.907 0 6.503-1.078 6.503-6.164 0-5.087-4.52-6.089-6.68-6.089m-.527 9.346H43.76l-.046-6.44h4.092c1.833 0 3.917.727 3.917 3.207s-2.059 3.233-3.892 3.233M64.863 18.176c-.443-.159-.553-.159-.957-.159-2.76 0-3.63 3.458-3.63 5.739v5.788h-2.964V15.612h2.913c0 .777.101 1.579-.301 3.157h.402c.702-2.33 2.184-3.557 4.118-3.557.452 0 1.36 0 2.191.478zM73.373 15.212c-4.219 0-7.157 3.057-7.157 7.392s2.938 7.367 7.157 7.367 7.131-3.032 7.131-7.367-2.912-7.392-7.13-7.392m0 11.952c-2.385 0-4.043-1.904-4.043-4.56s1.658-4.585 4.043-4.585 4.043 1.904 4.043 4.585-1.657 4.56-4.043 4.56M89.459 21.6l-1.155-.325c-1.206-.351-2.687-.802-2.687-2.055 0-.952.83-1.578 1.959-1.578 1.406 0 2.235.952 2.586 1.954l2.762-.902c-.603-1.98-2.536-3.483-5.223-3.483s-5.072 1.553-5.072 4.234 2.235 3.633 3.842 4.11l1.456.426c1.105.325 2.235.676 2.235 1.854 0 1.278-1.28 1.679-2.21 1.679-1.205 0-2.56-.727-3.213-2.355l-2.762.902c.678 2.33 2.938 3.91 6.001 3.91 3.24 0 5.349-1.805 5.349-4.436 0-2.806-2.486-3.533-3.867-3.933zM107.955 15.612h-2.988v7.592c0 2.08-1.205 3.96-3.314 3.96-1.733 0-2.888-1.304-2.888-3.86v-7.692h-2.988v8.745c0 3.884 2.134 5.613 5.022 5.613 2.059 0 3.441-.852 4.294-3.182h.402c-.402 1.278-.251 1.93-.05 2.756h2.963c-.276-1.103-.452-2.03-.452-4.36v-9.572zM119.119 15.212c-1.783 0-3.465.777-4.57 3.157h-.402c.402-1.278.402-2.005.402-2.756h-2.912v13.931h2.963v-7.366c0-2.606 1.481-4.16 3.49-4.16 2.135 0 2.988 1.755 2.988 4.06v7.467h2.988v-8.77c0-3.933-2.411-5.562-4.947-5.562zM138.52 25.184v-5.11c0-3.284-2.336-4.862-5.5-4.862-2.611 0-5.122 1.102-5.976 3.909l2.787.952c.477-1.628 1.632-2.43 3.039-2.43 1.607 0 2.611.977 2.611 2.155 0 1.303-1.255 1.403-4.218 2.03-2.511.525-4.671 1.578-4.671 4.209s2.235 3.934 4.621 3.934c2.033 0 3.666-1.053 4.444-3.133h.402c-.402 1.328-.277 2.03-.075 2.706h2.988c-.277-1.102-.452-2.054-.452-4.36m-3.014-.977c0 1.553-1.33 3.458-3.515 3.458-1.231 0-2.135-.601-2.135-1.73 0-1.352 1.532-1.903 2.763-2.179 1.104-.225 2.159-.526 2.887-1.002z"
            />
        </motion.svg>
    );
};

export default ShellDrawerLogo;
