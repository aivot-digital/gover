import {useLayoutEffect, useRef, type ReactNode} from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';

interface NoCodeTreeRowProps {
    up: boolean;
    down: boolean;
    operator?: boolean;
    contentSx?: SxProps<Theme>;
    children: ReactNode;
}

function findAnchor(content: HTMLElement): HTMLElement {
    // A nested expression connects to its own operator, not to its first (possibly deeply nested) operand.
    const expression = content.querySelector<HTMLElement>('[data-no-code-expression]');
    const operator = expression?.querySelector<HTMLElement>(':scope > [data-no-code-operator]');
    if (operator != null) return operator;

    const field = content.querySelector<HTMLElement>('[data-form-field-control-id]');
    const controlId = field?.dataset.formFieldControlId;
    let control = controlId == null ? null : content.ownerDocument.getElementById(controlId);
    if (field != null && control != null && field.contains(control)) {
        const controlRow = control.closest<HTMLElement>('[data-form-field-control]');
        if (controlRow?.parentElement === field) return controlRow;
        // The direct child is the visual control, independent of MUI's native/segmented input internals.
        while (control.parentElement !== field && control.parentElement != null) {
            control = control.parentElement;
        }
        return control;
    }

    return content.querySelector<HTMLElement>('[data-no-code-selector]') ?? content;
}

export function NoCodeTreeRow({up, down, operator, contentSx, children}: NoCodeTreeRowProps) {
    const rowRef = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);

    useLayoutEffect(() => {
        const row = rowRef.current;
        const content = contentRef.current;
        if (row == null || content == null) return;

        const updateAnchor = () => {
            const bounds = findAnchor(content).getBoundingClientRect();
            const offset = bounds.top + bounds.height / 2 - row.getBoundingClientRect().top;
            row.style.setProperty('--no-code-connector-y', `${offset}px`);
        };
        const resizeObserver = typeof ResizeObserver === 'undefined' ? undefined : new ResizeObserver(updateAnchor);
        const observeAnchor = () => {
            resizeObserver?.disconnect();
            // Siblings can move the anchor without changing its size (labels, hints and preceding operands).
            let element: HTMLElement | null = findAnchor(content);
            while (element != null && content.contains(element)) {
                resizeObserver?.observe(element);
                for (const child of element.children) resizeObserver?.observe(child);
                if (element === content) break;
                element = element.parentElement;
            }
            updateAnchor();
        };

        observeAnchor();
        const mutationObserver = new MutationObserver(observeAnchor);
        mutationObserver.observe(content, {
            childList: true,
            subtree: true,
            characterData: true,
            attributes: true,
            attributeFilter: ['id', 'data-form-field-control-id'],
        });
        return () => {
            resizeObserver?.disconnect();
            mutationObserver.disconnect();
        };
    }, []);

    return (
        <Box
            ref={rowRef}
            data-no-code-tree-row
            data-no-code-operator={operator || undefined}
            sx={{display: 'flex', alignItems: 'stretch', pl: '0.25rem', minWidth: 0}}
        >
            <Box aria-hidden="true" sx={{position: 'relative', width: '1.5rem', flexShrink: 0}}>
                {up && <Box sx={{position: 'absolute', left: '50%', top: 0, bottom: 'calc(100% - var(--no-code-connector-y, 50%))', width: '1px', bgcolor: 'divider'}}/>}
                {down && <Box sx={{position: 'absolute', left: '50%', top: 'var(--no-code-connector-y, 50%)', bottom: 0, width: '1px', bgcolor: 'divider'}}/>}
                <Box sx={{position: 'absolute', left: '50%', right: 0, top: 'var(--no-code-connector-y, 50%)', height: '1px', bgcolor: 'divider'}}/>
            </Box>
            <Box ref={contentRef} sx={[{flex: 1, minWidth: 0}, ...(Array.isArray(contentSx) ? contentSx : [contentSx])]}>
                {children}
            </Box>
        </Box>
    );
}
