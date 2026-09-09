import {createRef} from 'react';
import {act, render, waitFor} from '@testing-library/react';
import {createTheme} from '@mui/material/styles';
import {describe, expect, it, vi} from 'vitest';
import {DynamicTextField, type DynamicTextFieldMethods} from './dynamic-text-field';
import {getDynamicTextTokenStyles} from './dynamic-text-lexical';

describe('DynamicTextField', () => {
    it('matches the shared one-line input dimensions', () => {
        const {container} = render(
            <DynamicTextField
                id="template"
                ariaLabelledBy="template-label"
                value="Text"
                onChange={vi.fn()}
            />,
        );

        const control = container.firstElementChild as HTMLElement;
        const editor = container.querySelector<HTMLElement>('[contenteditable="true"]');

        expect(getComputedStyle(control).boxSizing).toBe('border-box');
        expect(getComputedStyle(control).minHeight).toBe('44px');
        expect(getComputedStyle(control).height).toBe('44px');
        expect(editor?.style.height).toBe('42px');
        expect(editor?.style.padding).toBe('9.5px 14px');
        expect(editor?.style.lineHeight).toBe('23px');
    });

    it('renders complete expressions and directives as editable tokens', async () => {
        const {container} = render(
            <DynamicTextField
                id="template"
                ariaLabelledBy="template-label"
                multiline
                required
                invalid
                value="{% if $.enabled %}Hallo {{ $.name }}{% endif %}"
                variableMetadata={[{
                    reference: '$.enabled',
                    label: 'Aktiviert',
                    category: 'Vorgangsdaten',
                    origin: 'Antrag prüfen',
                    description: 'Ergebnis der Prüfung',
                }]}
                onChange={vi.fn()}
            />,
        );

        await waitFor(() => {
            expect(container.querySelectorAll('.dynamic-text-token')).toHaveLength(3);
        });

        const editor = container.querySelector<HTMLElement>('[contenteditable="true"]');
        const directive = container.querySelector<HTMLElement>('[data-dynamic-text-kind="directive"]');

        expect(editor).toContainElement(directive);
        expect(editor).toHaveAttribute('aria-multiline', 'true');
        expect(editor).toHaveAttribute('aria-required', 'true');
        expect(editor).toHaveAttribute('aria-invalid', 'true');
        expect(editor).toHaveAttribute('spellcheck', 'false');
        expect(directive).not.toHaveAttribute('contenteditable', 'false');
        expect(directive).toHaveAttribute('spellcheck', 'false');
        await waitFor(() => {
            expect(directive).toHaveAttribute(
                'title',
                'Aktiviert\nVorgangsdaten · Erzeugt von Antrag prüfen\nErgebnis der Prüfung\n$.enabled',
            );
        });
        expect(directive)
            .toHaveTextContent('{% if $.enabled %}');
        expect(container.querySelector('[data-dynamic-text-kind="output"]'))
            .toHaveTextContent('{{ $.name }}');
        expect(container.querySelector('[contenteditable="true"]')).toHaveTextContent(
            '{% if $.enabled %}Hallo {{ $.name }}{% endif %}',
        );
    });

    it('does not style an unfinished expression as a valid token', async () => {
        const {container} = render(
            <DynamicTextField
                id="template"
                ariaLabelledBy="template-label"
                value="Hallo {{ $.name"
                onChange={vi.fn()}
            />,
        );

        await waitFor(() => {
            expect(container.querySelector('[contenteditable="true"]')).toHaveTextContent('Hallo {{ $.name');
        });
        expect(container.querySelector('.dynamic-text-token')).not.toBeInTheDocument();
    });

    it('inserts a variable reference without exposing delimiter construction to consumers', async () => {
        const ref = createRef<DynamicTextFieldMethods>();
        const {container} = render(
            <DynamicTextField
                ref={ref}
                id="template"
                ariaLabelledBy="template-label"
                value=""
                onChange={vi.fn()}
            />,
        );

        act(() => ref.current?.insertVariableReference('$.name'));

        await waitFor(() => {
            expect(container.querySelector('.dynamic-text-token')).toHaveTextContent('{{ $.name }}');
        });
    });

    it('provides wrapping and forced-colors fallbacks for highlighted tokens', () => {
        const styles = getDynamicTextTokenStyles(createTheme()) as {
            '@container (max-width: 360px)': Record<string, Record<string, unknown>>;
            '@media (forced-colors: active)': Record<string, Record<string, unknown>>;
        };

        expect(styles['@container (max-width: 360px)']
            ['&[data-dynamic-text-multiline="true"] .dynamic-text-token'])
            .toMatchObject({
                whiteSpace: 'normal',
                overflowWrap: 'anywhere',
                boxDecorationBreak: 'slice',
                WebkitBoxDecorationBreak: 'slice',
            });
        expect(styles['@media (forced-colors: active)']['& .dynamic-text-token'])
            .toMatchObject({
                color: 'CanvasText',
                WebkitTextFillColor: 'CanvasText',
                backgroundImage: 'none',
            });
    });

    it('registers exact syntax ranges when the Custom Highlight API is available', async () => {
        const registeredHighlights = new Map<string, Highlight>();
        const highlights = {
            clear: vi.fn(),
            delete: vi.fn((name: string) => registeredHighlights.delete(name)),
            entries: vi.fn(),
            forEach: vi.fn(),
            get: vi.fn((name: string) => registeredHighlights.get(name)),
            has: vi.fn((name: string) => registeredHighlights.has(name)),
            keys: vi.fn(),
            set: vi.fn((name: string, highlight: Highlight) => {
                registeredHighlights.set(name, highlight);
                return highlights;
            }),
            size: 0,
            values: vi.fn(),
        };

        class TestHighlight extends Set<AbstractRange> {
            priority = 0;
            type: HighlightType = 'highlight';

            constructor(...ranges: AbstractRange[]) {
                super(ranges);
            }
        }

        vi.stubGlobal('CSS', {highlights});
        vi.stubGlobal('Highlight', TestHighlight);

        const {container, unmount} = render(
            <DynamicTextField
                id="template"
                ariaLabelledBy="template-label"
                multiline
                value="{% if $.enabled %}"
                onChange={vi.fn()}
            />,
        );

        await waitFor(() => {
            expect(container.firstElementChild)
                .toHaveAttribute('data-dynamic-text-range-highlighting', 'true');
        });

        const keywordRanges = registeredHighlights.get('prosuna-dynamic-text-keyword');
        const propertyRanges = registeredHighlights.get('prosuna-dynamic-text-property');
        expect(Array.from(keywordRanges ?? [], (range) => range.toString())).toEqual(['if']);
        expect(Array.from(propertyRanges ?? [], (range) => range.toString())).toEqual(['$.enabled']);

        unmount();
        vi.unstubAllGlobals();
    });
});
