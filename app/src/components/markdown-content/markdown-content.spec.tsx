import React from 'react';
import {Box, ThemeProvider} from '@mui/material';
import {getContrastRatio} from '@mui/material/styles';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {BaseTheme} from '../../theming/base-theme';
import {createDefaultAppTheme} from '../../theming/themes';
import {MarkdownContent} from './markdown-content';

describe('MarkdownContent', () => {
    it.each(['light', 'dark'] as const)('keeps inline and block code readable in %s mode', (mode) => {
        const theme = createDefaultAppTheme(BaseTheme, mode);
        const {container} = render(
            <ThemeProvider theme={theme}>
                <Box sx={{color: 'text.secondary', bgcolor: 'background.paper'}}>
                    <MarkdownContent markdown={'Use `GET`.\n\n```sh\ncurl --request GET\n```'}/>
                </Box>
            </ThemeProvider>,
        );

        const inlineCode = screen.getByText('GET');
        const pre = container.querySelector('pre')!;
        const blockCode = pre.querySelector('code')!;

        for (const element of [inlineCode, pre]) {
            expect(element).toHaveStyle({
                color: theme.palette.text.primary,
                backgroundColor: theme.palette.background.default,
            });
            const style = window.getComputedStyle(element);
            expect(getContrastRatio(style.color, style.backgroundColor)).toBeGreaterThanOrEqual(4.5);
        }

        expect(blockCode).toHaveStyle({
            color: theme.palette.text.primary,
            backgroundColor: 'rgba(0, 0, 0, 0)',
            padding: '0px',
        });
    });

    it('allows long inline values to wrap without changing their content', () => {
        const key = 'test-key-'.repeat(30);
        render(<MarkdownContent markdown={`Use the key \`${key}\`.`}/>);

        const code = screen.getByText(key);
        expect(code.tagName).toBe('CODE');
        expect(code.textContent).toBe(key);
        expect(code).toHaveStyle({overflowWrap: 'anywhere', wordBreak: 'normal'});
    });

    it('preserves code block whitespace and special characters while allowing horizontal scrolling', () => {
        const command = [
            'curl \\',
            '  --url "https://example.org/hook/?first=1&second=2" \\',
            "  --data '<root>value</root>'",
        ].join('\n');
        const {container} = render(<MarkdownContent markdown={`\`\`\`sh\n${command}\n\`\`\``}/>);

        const pre = container.querySelector('pre')!;
        const code = pre.querySelector('code')!;
        expect(code.textContent).toBe(`${command}\n`);
        expect(code.querySelector('root')).toBeNull();
        expect(pre).toHaveStyle({
            whiteSpace: 'pre',
            wordBreak: 'normal',
            overflowWrap: 'normal',
            overflowX: 'auto',
        });
        expect(code).toHaveStyle({overflowWrap: 'normal'});
    });
});
