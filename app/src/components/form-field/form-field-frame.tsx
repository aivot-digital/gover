import {useLayoutEffect, useRef, type ReactNode} from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import {formFieldRootSx, getFormFieldMarginSx, type FormFieldMargin} from '../../theming/form-field-tokens';

interface FormFieldFrameProps {
    externalAction?: ReactNode;
    margin: FormFieldMargin;
    sx?: SxProps<Theme>;
    children: (sx: SxProps<Theme>) => ReactNode;
}

// Use component width, not viewport width: fields also appear in narrow node-editor panels.
const inlineActionQuery = '@container field-action (min-width: 30rem)';

export function hasExternalAction(action: ReactNode): boolean {
    return action != null && action !== false && action !== '';
}

/** Shares the control row with an independent action without including that action in field semantics. */
export function FormFieldFrame({externalAction, margin, sx, children}: FormFieldFrameProps) {
    const gridRef = useRef<HTMLDivElement>(null);
    const hasAction = hasExternalAction(externalAction);
    useLayoutEffect(() => {
        const grid = gridRef.current;
        if (grid == null) return;
        const field = grid.firstElementChild;
        const action = grid.lastElementChild;
        if (!(field instanceof HTMLElement) || !(action instanceof HTMLElement)) return;

        // Native fieldsets have an anonymous content box that cannot share subgrid rows.
        // Measure the actual control instead of estimating its center from label/helper heights.
        const updateAnchor = () => {
            const control = field.querySelector<HTMLElement>(':scope > [data-form-field-control]');
            if (control == null) return;
            // The positioned field is the direct control wrapper's offset parent. Layout coordinates
            // stay unscaled inside zoomable editors; viewport rectangles would apply the zoom twice.
            const offset = control.offsetTop + (control.offsetHeight - action.offsetHeight) / 2;
            grid.style.setProperty('--field-action-offset', `${Math.max(0, offset)}px`);
        };
        const observer = typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(updateAnchor);
        const observeRows = () => {
            observer?.disconnect();
            observer?.observe(field);
            observer?.observe(action);
            for (const row of field.children) observer?.observe(row);
            updateAnchor();
        };
        observeRows();
        // Observe new rows, not every DOM mutation inside rich-text/code editors. Their row's
        // ResizeObserver already catches size changes without resubscribing on each keystroke.
        const mutations = new MutationObserver(observeRows);
        mutations.observe(field, {childList: true});
        return () => {
            observer?.disconnect();
            mutations.disconnect();
        };
    }, [hasAction]);

    const rootSx = [formFieldRootSx, getFormFieldMarginSx(margin), ...(Array.isArray(sx) ? sx : [sx])];
    if (!hasAction) return children(rootSx);

    return (
        <Box sx={[...rootSx, {containerType: 'inline-size', containerName: 'field-action'}]}>
            <Box ref={gridRef} sx={{
                display: 'grid',
                gridTemplateColumns: 'minmax(0, 1fr)',
                alignItems: 'start',
                minWidth: 0,
                [inlineActionQuery]: {
                    gridTemplateColumns: 'minmax(0, 1fr) fit-content(40%)',
                    columnGap: 2,
                },
            }}>
                {children({
                    ...formFieldRootSx,
                    gridColumn: 1,
                    gridRow: 1,
                    '& > [data-form-field-control]': {minWidth: 0},
                })}
                <Box data-form-field-external-action sx={{
                    gridColumn: 1,
                    gridRow: 2,
                    mt: 1.5,
                    minWidth: 0,
                    maxWidth: '100%',
                    justifySelf: 'start',
                    '& .MuiButton-root': {whiteSpace: 'normal', overflowWrap: 'anywhere'},
                    [inlineActionQuery]: {
                        gridColumn: 2,
                        gridRow: 1,
                        mt: 'var(--field-action-offset, 0px)',
                    },
                }}>
                    {/* Keep this after the helper in DOM order and outside disabled fieldsets.
                        Callers own its disabled state; a read-only field may still offer an independent action. */}
                    {externalAction}
                </Box>
            </Box>
        </Box>
    );
}
