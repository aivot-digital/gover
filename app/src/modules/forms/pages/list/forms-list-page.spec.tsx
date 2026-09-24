import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Actions} from '../../../../components/actions/actions';
import type {Action} from '../../../../components/actions/actions-props';
import {FormTriggerApiService, type FormOverviewItem} from '../../services/form-trigger-api-service';
import {FormsListPage} from './forms-list-page';

const mocks = vi.hoisted(() => ({
    dispatch: vi.fn(),
    downloadBlobFile: vi.fn(),
    downloadQrCode: vi.fn(),
    copyToClipboardText: vi.fn(),
    canRead: true,
    form: null as FormOverviewItem | null,
}));

vi.mock('../../../../components/generic-list-page/generic-list-page', () => ({
    GenericListPage: ({rowActions, columnDefinitions}: {
        rowActions: (item: FormOverviewItem, permissions: unknown) => Action[];
        columnDefinitions: Array<{
            field: string;
            renderCell?: (params: {row: FormOverviewItem}) => React.ReactNode;
        }>;
    }) => (
        <>
            {columnDefinitions.map((column) => (
                <div key={column.field}>{column.renderCell?.({row: mocks.form!})}</div>
            ))}
            <Actions
                actions={rowActions(mocks.form!, {
                    canRead: () => mocks.canRead,
                    canUpdate: () => true,
                })}
                dense
            />
        </>
    ),
}));

vi.mock('../../../../components/page-wrapper/page-wrapper', () => ({
    PageWrapper: ({children}: {children: React.ReactNode}) => <>{children}</>,
}));

vi.mock('../../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => mocks.dispatch}));
vi.mock('../../../../hooks/use-app-selector', () => ({useAppSelector: () => []}));
vi.mock('../../../../utils/download-utils', () => ({downloadBlobFile: mocks.downloadBlobFile}));
vi.mock('../../../../utils/download-qrcode', () => ({downloadQrCode: mocks.downloadQrCode}));
vi.mock('../../../../utils/copy-to-clipboard', () => ({copyToClipboardText: mocks.copyToClipboardText}));

function renderPage() {
    return render(<MemoryRouter><FormsListPage /></MemoryRouter>);
}

describe('FormsListPage actions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mocks.canRead = true;
        mocks.form = {
            id: 17,
            nodeName: 'Formulareingang',
            formTitle: 'Online-Antrag',
            formSlug: 'antrag',
            processId: 42,
            processTitle: 'Hundesteuer',
            processVersion: 2,
            status: 'Published',
            publicUrl: 'https://example.test/form/hundesteuer/antrag/',
            showOnFormIndexPage: true,
            updated: '2026-09-01T10:00:00Z',
            published: '2026-09-01T10:00:00Z',
        };
    });

    it('keeps the process action and downloads the saved PDF from the list', async () => {
        const blob = new Blob(['pdf'], {type: 'application/pdf'});
        const download = vi.spyOn(FormTriggerApiService.prototype, 'downloadPrintablePdf').mockResolvedValue(blob);

        renderPage();

        expect(screen.getByRole('link', {name: 'Prozess ansehen'})).toHaveAttribute(
            'href', '/processes/42/versions/2',
        );
        expect(screen.queryByRole('button', {name: 'Vordruck herunterladen (PDF)'})).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        fireEvent.click(screen.getByRole('menuitem', {name: 'Vordruck herunterladen (PDF)'}));

        await waitFor(() => {
            expect(download).toHaveBeenCalledWith(17);
            expect(mocks.downloadBlobFile).toHaveBeenCalledWith('Online-Antrag.pdf', blob);
        });
        download.mockRestore();
    });

    it('offers the public link and QR code in the row menu', async () => {
        mocks.copyToClipboardText.mockResolvedValue(true);
        mocks.downloadQrCode.mockResolvedValue(undefined);
        renderPage();

        expect(screen.getByText('Formulareingang · /antrag')).toBeInTheDocument();
        expect(screen.getByText('Im Formularverzeichnis')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        fireEvent.click(screen.getByRole('menuitem', {name: 'Öffentlichen Link kopieren'}));
        await waitFor(() => expect(mocks.copyToClipboardText).toHaveBeenCalledWith(mocks.form!.publicUrl));

        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        fireEvent.click(screen.getByRole('menuitem', {name: 'QR-Code mit öffentlichem Link herunterladen'}));
        await waitFor(() => expect(mocks.downloadQrCode).toHaveBeenCalledWith(
            mocks.form!.publicUrl,
            'qr-code-17.png',
        ));

        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        expect(screen.queryByRole('menuitem', {name: 'Vorgänge aus dem Formular anzeigen'})).not.toBeInTheDocument();
    });

    it('does not offer a public link or QR code for a draft', () => {
        mocks.form = {...mocks.form!, status: 'Drafted', publicUrl: null, formSlug: null};
        renderPage();

        expect(screen.getByText('Formulareingang')).toBeInTheDocument();
        expect(screen.getByText('In Bearbeitung')).toBeInTheDocument();
        expect(screen.queryByText('Im Formularverzeichnis')).not.toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Prozess ansehen'})).toBeInTheDocument();
        expect(screen.queryByRole('link', {name: 'Formular öffnen (in neuem Tab)'})).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));
        expect(screen.getByRole('menuitem', {name: 'Vordruck herunterladen (PDF)'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Öffentlichen Link kopieren'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'QR-Code mit öffentlichem Link herunterladen'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Vorgänge aus dem Formular anzeigen'})).not.toBeInTheDocument();
    });

    it('does not offer the printable PDF without process read permission', () => {
        mocks.canRead = false;
        renderPage();

        fireEvent.click(screen.getByRole('button', {name: 'Weitere Optionen'}));

        expect(screen.queryByRole('menuitem', {name: 'Vordruck herunterladen (PDF)'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Vorgänge aus dem Formular anzeigen'})).not.toBeInTheDocument();
    });

    it('hides the row menu when no options are available', () => {
        mocks.canRead = false;
        mocks.form = {...mocks.form!, status: 'Drafted', publicUrl: null};
        renderPage();

        expect(screen.queryByRole('button', {name: 'Weitere Optionen'})).not.toBeInTheDocument();
    });

    it('shows when a published form is available only through its direct link', () => {
        mocks.form = {...mocks.form!, showOnFormIndexPage: false};
        renderPage();

        expect(screen.getByText('Nur per Direktlink')).toBeInTheDocument();
        expect(screen.queryByText('Im Formularverzeichnis')).not.toBeInTheDocument();
    });

    it('keeps long cell labels on one line while retaining their full text', () => {
        mocks.form = {
            ...mocks.form!,
            formTitle: 'Ein sehr langer öffentlicher Formulartitel für die Übersicht',
            nodeName: 'Ein langer interner Name',
            formSlug: 'ein-langes-formularsegment',
            processTitle: 'Ein sehr langer Prozessname für die Übersicht',
        };
        renderPage();

        const formLink = screen.getByRole('link', {name: mocks.form.formTitle});
        const processLink = screen.getByRole('link', {name: mocks.form.processTitle});
        expect(formLink).toHaveAttribute('title', mocks.form.formTitle);
        expect(processLink).toHaveAttribute('title', mocks.form.processTitle);

        for (const element of [
            formLink.parentElement,
            processLink.parentElement,
            screen.getByText('Ein langer interner Name · /ein-langes-formularsegment'),
            screen.getByText('Version 2'),
            screen.getByText('Veröffentlicht'),
            screen.getByText('Im Formularverzeichnis'),
            screen.getByText('Veröffentlicht am'),
        ]) {
            expect(element).toHaveClass('MuiTypography-noWrap');
        }
    });
});
