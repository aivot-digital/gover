import {type Transition} from 'motion/react';

export const SHELL_DRAWER_TRANSITION: Transition = {
    type: 'tween',
    duration: 0.24,
    ease: [
        0.4,
        0,
        0.2,
        1,
    ],
};

export const SHELL_DRAWER_REDUCED_MOTION_TRANSITION: Transition = {
    duration: 0,
};

export const SHELL_DRAWER_CONTENT_SWITCH_DELAY_MS = 80;

export const SHELL_DRAWER_CONTENT_TRANSITION: Transition = {
    duration: 0.24,
    times: [
        0,
        0.28,
        0.38,
        1,
    ],
    ease: [
        0.4,
        0,
        0.2,
        1,
    ],
};
