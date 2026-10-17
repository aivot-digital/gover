import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {FileUploadFileList, FileUploadHelper} from './file-upload-field-layout';

describe('FileUploadHelper', () => {
    it('should present a maximum file count as a limit rather than a target', () => {
        render(
            <FileUploadHelper
                fileCount={2}
                maxFiles={3}
            />,
        );

        expect(screen.getByText('2 von max. 3 Dateien')).toBeInTheDocument();
    });

    it('should present equal minimum and maximum counts as an exact target', () => {
        render(
            <FileUploadHelper
                fileCount={2}
                minFiles={3}
                maxFiles={3}
            />,
        );

        expect(screen.getByText('2 von 3 Dateien')).toBeInTheDocument();
    });
});

describe('FileUploadFileList', () => {
    it('should truncate long file names while retaining the complete title', () => {
        const fileName = 'The Art of Data - Jeff Desjardins & Nick Routley (Visual Capitalist).pdf';

        render(
            <FileUploadFileList
                items={[{
                    key: 'file',
                    name: fileName,
                    size: '51.01 MB',
                    actionLabel: 'Datei entfernen',
                    actionIcon: <span />,
                    onAction: () => undefined,
                }]}
            />,
        );

        const fileNameElement = screen.getByTitle(fileName);
        expect(fileNameElement).toHaveStyle({
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
        });
    });
});
