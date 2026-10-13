import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {cleanup, render, screen, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {type DashboardInstance, type DashboardInstanceSummary} from '../models/dashboard-overview';
import {DASHBOARD_INSTANCES_LIST_PATH, DashboardInstancesPanel} from './dashboard-instances-panel';

const instance = (id: number, processTitle: string, deadline: string | null): DashboardInstance => ({
    id,
    processId: 7,
    processVersion: 2,
    processTitle,
    caseNumber: `7K0M-9X1Q-000${id}`,
    started: '2026-10-01T08:00:00Z',
    deadline,
});

const summary: DashboardInstanceSummary = {
    total: 6,
    overdue: 1,
    items: [
        instance(1, 'Gewerbe anmelden', '2026-10-11T08:00:00Z'),
        instance(2, 'Baumfällung beantragen', '2026-10-15T08:00:00Z'),
        instance(3, 'Wohngeld beantragen', '2026-10-31T08:00:00Z'),
        instance(4, 'Fundsache melden', null),
    ],
};

function renderPanel(value: DashboardInstanceSummary, previewOnly = false) {
    return render(
        <MemoryRouter>
            <DashboardInstancesPanel summary={value} previewOnly={previewOnly}/>
        </MemoryRouter>,
    );
}

describe('DashboardInstancesPanel', () => {
    beforeEach(() => {
        // Only the clock is faked; started 2026-10-01, so the halfway point of a 14-day deadline has passed.
        vi.useFakeTimers({toFake: ['Date']});
        vi.setSystemTime(new Date('2026-10-12T10:00:00Z'));
    });

    afterEach(() => {
        cleanup();
        vi.useRealTimers();
    });

    it('summarizes running and overdue instances', () => {
        renderPanel(summary);

        expect(screen.getByRole('heading', {name: 'Meine Vorgänge'})).toBeVisible();
        expect(screen.getByText('6 laufend')).toBeVisible();
        expect(screen.getByText('1 Frist überschritten')).toBeVisible();
    });

    it('pluralizes several overdue instances and omits the overdue count without any', () => {
        const {unmount} = renderPanel({...summary, overdue: 2});
        expect(screen.getByText('2 Fristen überschritten')).toBeVisible();
        unmount();

        renderPanel({...summary, overdue: 0});
        expect(screen.queryByText(/überschritten$/)).not.toBeInTheDocument();
    });

    it.each([
        {title: 'Gewerbe anmelden', label: 'Überschritten · 11.10.2026'},
        {title: 'Baumfällung beantragen', label: 'Bald fällig · 15.10.2026'},
        {title: 'Wohngeld beantragen', label: 'Frist · 31.10.2026'},
    ])('labels the deadline state of $title', ({title, label}) => {
        renderPanel(summary);

        const link = screen.getByRole('link', {name: new RegExp(title)});
        expect(within(link).getByText(label)).toBeVisible();
    });

    it('links each instance and the filtered instance list', () => {
        renderPanel(summary);

        const link = screen.getByRole('link', {name: /Fundsache melden/});
        expect(link).toHaveAttribute('href', '/process-instances/4');
        expect(link).toHaveTextContent('7K0M-9X1Q-0004 · Eingegangen am 01.10.2026');
        expect(link).not.toHaveTextContent('Frist');
        expect(screen.getByRole('link', {name: 'Alle Vorgänge ansehen'}))
            .toHaveAttribute('href', DASHBOARD_INSTANCES_LIST_PATH);
    });

    it('does not link instances in the preview', () => {
        renderPanel(summary, true);

        expect(screen.queryByRole('link', {name: /Gewerbe anmelden/})).not.toBeInTheDocument();
        expect(screen.getAllByRole('listitem')).toHaveLength(4);
    });
});
