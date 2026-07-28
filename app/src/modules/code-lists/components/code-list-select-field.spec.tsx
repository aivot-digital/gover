import {render, screen, waitFor} from '@testing-library/react';
import {CodeListSelectField} from './code-list-select-field';
import {CodeListsApiService} from '../code-lists-api-service';

jest.mock('../code-lists-api-service', () => ({
    CodeListsApiService: jest.fn(),
}));

jest.mock('../../../components/select-field-2/select-field-component', () => ({
    SelectFieldComponent: ({label, options, value}: any) => (
        <div
            aria-label={label}
            data-testid="code-list-select"
            data-value={value ?? ''}
            data-options={options.map((option: any) => option.value).join(',')}
        />
    ),
}));

describe('CodeListSelectField', () => {
    const listAllOrdered = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        (CodeListsApiService as unknown as jest.Mock).mockImplementation(() => ({
            listAllOrdered,
        }));
    });

    it('should keep stale code list references so the form can show the missing-reference error', async () => {
        listAllOrdered.mockResolvedValue({
            content: [
                {
                    key: 'existing',
                    name: 'Existing',
                    description: '',
                },
            ],
        });
        const onChange = jest.fn();

        render(
            <CodeListSelectField
                value="deleted"
                onChange={onChange}
            />,
        );

        await waitFor(() => {
            expect(screen.getByTestId('code-list-select')).toHaveAttribute('data-options', 'existing');
        });

        expect(onChange).not.toHaveBeenCalled();
    });
});
