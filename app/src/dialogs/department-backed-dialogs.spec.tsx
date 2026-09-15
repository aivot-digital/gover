import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AccessibilityDialog} from './accessibility-dialog/accessibility-dialog';
import {HelpDialog} from './help-dialog/help.dialog';
import {ImprintDialog} from './imprint-dialog/imprint-dialog';
import {PrivacyDialog} from './privacy-dialog/privacy-dialog';
import {type PublicDepartmentResponseDTO} from '../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../modules/departments/services/department-api-service';
import {ProcessDefinitionVersionApiService} from '../modules/process/services/process-definition-version-api-service';

const mocks = vi.hoisted(() => ({
    listingDepartmentId: '900',
}));

vi.mock('../hooks/use-app-selector', () => ({
    useAppSelector: () => mocks.listingDepartmentId,
}));

describe('department-backed dialogs', () => {
    beforeEach(() => {
        mocks.listingDepartmentId = '900';
    });

    it('uses direct department IDs before IDs from the process version', async () => {
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            accessibilityDepartmentId: 11,
            privacyDepartmentId: 12,
            imprintDepartmentId: 13,
            processSpecificAccessibilityStatement: 'Prozessspezifische Barrierefreiheit',
            processSpecificPrivacyStatement: 'Prozessspezifischer Datenschutz',
        };
        const retrievePublic = vi.spyOn(DepartmentApiService.prototype, 'retrievePublic')
            .mockImplementation(async (departmentId) => createDepartment(departmentId));

        render(
            <>
                <AccessibilityDialog
                    open
                    onHide={vi.fn()}
                    version={version}
                    departmentId={21}
                />
                <PrivacyDialog
                    open
                    onHide={vi.fn()}
                    version={version}
                    departmentId={22}
                />
                <ImprintDialog
                    open
                    onHide={vi.fn()}
                    version={version}
                    departmentId={23}
                    isListingPage
                />
            </>,
        );

        expect(await screen.findByText('Barrierefreiheit aus Department 21')).toBeInTheDocument();
        expect(await screen.findByText('Datenschutz aus Department 22')).toBeInTheDocument();
        expect(await screen.findByText('Impressum aus Department 23')).toBeInTheDocument();
        expect(screen.getByText('Prozessspezifische Barrierefreiheit')).toBeInTheDocument();
        expect(screen.getByText('Prozessspezifischer Datenschutz')).toBeInTheDocument();
        expect(retrievePublic).toHaveBeenCalledWith(21);
        expect(retrievePublic).toHaveBeenCalledWith(22);
        expect(retrievePublic).toHaveBeenCalledWith(23);
        expect(retrievePublic).not.toHaveBeenCalledWith(11);
        expect(retrievePublic).not.toHaveBeenCalledWith(12);
        expect(retrievePublic).not.toHaveBeenCalledWith(13);
        expect(retrievePublic).not.toHaveBeenCalledWith(900);
    });

    it('keeps resolving listing-page department IDs when no direct ID is provided', async () => {
        const retrievePublic = vi.spyOn(DepartmentApiService.prototype, 'retrievePublic')
            .mockImplementation(async (departmentId) => createDepartment(departmentId));

        render(
            <ImprintDialog
                open
                onHide={vi.fn()}
                isListingPage
            />,
        );

        expect(await screen.findByText('Impressum aus Department 900')).toBeInTheDocument();
        expect(retrievePublic).toHaveBeenCalledWith(900);
    });

    it('loads separate direct departments for technical and specialist support without a form', async () => {
        const version = {
            ...ProcessDefinitionVersionApiService.initialize(),
            publicTitle: 'Bestandsformular',
            technicalSupportDepartmentId: 31,
            legalSupportDepartmentId: 32,
        };
        const retrievePublic = vi.spyOn(DepartmentApiService.prototype, 'retrievePublic')
            .mockImplementation(async (departmentId) => createDepartment(departmentId));

        render(
            <HelpDialog
                open
                onHide={vi.fn()}
                version={version}
                technicalSupportDepartmentId={41}
                legalSupportDepartmentId={42}
            />,
        );

        expect(await screen.findByText('technical-41@example.test')).toBeInTheDocument();
        expect(await screen.findByText('special-42@example.test')).toBeInTheDocument();
        await waitFor(() => {
            expect(retrievePublic).toHaveBeenCalledWith(41);
            expect(retrievePublic).toHaveBeenCalledWith(42);
        });
        expect(retrievePublic).not.toHaveBeenCalledWith(31);
        expect(retrievePublic).not.toHaveBeenCalledWith(32);
        expect(screen.getByText('technical-41@example.test').closest('a'))
            .toHaveAttribute('href', 'mailto:technical-41@example.test?subject=Technische%20Hilfe%3A%20Bestandsformular');
        expect(screen.getByText('special-42@example.test').closest('a'))
            .toHaveAttribute('href', 'mailto:special-42@example.test?subject=Fachliche%20Hilfe%3A%20Bestandsformular');
    });
});

function createDepartment(departmentId: number): PublicDepartmentResponseDTO {
    return {
        id: departmentId,
        commonAccessibility: `Barrierefreiheit aus Department ${departmentId}`,
        commonPrivacy: `Datenschutz aus Department ${departmentId}`,
        imprint: `Impressum aus Department ${departmentId}`,
        technicalSupportEmail: `technical-${departmentId}@example.test`,
        specialSupportEmail: `special-${departmentId}@example.test`,
    };
}
