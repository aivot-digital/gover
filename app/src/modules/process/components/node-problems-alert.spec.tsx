import {render, screen, within} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {NodeProblemsAlert} from './node-problems-alert';

describe('NodeProblemsAlert', () => {
    it.each(['test', 'publish'] as const)('includes version-only problems in one %s summary', (mode) => {
        render(<NodeProblemsAlert mode={mode} versionProblems={['Öffentlicher Titel fehlt.']} problems={[]} availableNodeProviders={[]}/>);

        const alert = screen.getByRole('alert');
        expect(screen.getAllByRole('alert')).toHaveLength(1);
        expect(within(alert).getByText('Versionsspezifische Einstellungen:')).toBeVisible();
        expect(within(alert).getByText('Öffentlicher Titel fehlt.')).toBeVisible();
        expect(alert).toHaveClass(mode === 'test' ? 'MuiAlert-colorWarning' : 'MuiAlert-colorError');
        expect(alert).toHaveTextContent(mode === 'test' ? 'Sie können den Test starten' : 'Sie können den Prozess erst veröffentlichen');
    });
});
