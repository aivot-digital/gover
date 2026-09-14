import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../data/element-type/element-type';
import {CommunicationProvidersApiService} from './communication-providers-api-service';
import {type CommunicationTestingLayout, type CommunicationTestResultLayout} from './models';

const testingLayout = {
    type: ElementType.GroupLayout,
    id: 'communication-provider-test',
    name: null,
    testProtocolSet: null,
    visibility: null,
    override: null,
    metadata: null,
    weight: null,
    children: [],
    marketplaceLink: null,
} satisfies CommunicationTestingLayout;

const testResultLayout = {
    ...testingLayout,
    id: 'communication-provider-test-result',
} satisfies CommunicationTestResultLayout;

describe('CommunicationProvidersApiService', () => {
    it('loads and parses the provider testing layout', async () => {
        const service = new CommunicationProvidersApiService();
        const fetchMock = vi.spyOn(service, 'fetch').mockResolvedValue(responseWithText(JSON.stringify(testingLayout)));

        await expect(service.getProviderTestingLayout(7)).resolves.toEqual(testingLayout);
        expect(fetchMock).toHaveBeenCalledWith('GET', '/api/communication-providers/7/test/');
    });

    it('maps an empty testing-layout response to null', async () => {
        const service = new CommunicationProvidersApiService();
        vi.spyOn(service, 'fetch').mockResolvedValue(responseWithText(''));

        await expect(service.getProviderTestingLayout(7)).resolves.toBeNull();
    });

    it('posts the test inputs and parses the result layout', async () => {
        const service = new CommunicationProvidersApiService();
        const fetchMock = vi.spyOn(service, 'fetch').mockResolvedValue(responseWithJson(testResultLayout));
        const inputs = {'test-recipient': 'test@example.com'};

        await expect(service.testProvider(7, inputs)).resolves.toEqual(testResultLayout);
        expect(fetchMock).toHaveBeenCalledWith(
            'POST',
            '/api/communication-providers/7/test/',
            JSON.stringify(inputs),
            undefined,
        );
    });
});

function responseWithText(body: string): Response {
    return {
        text: vi.fn().mockResolvedValue(body),
    } as unknown as Response;
}

function responseWithJson(body: unknown): Response {
    return {
        json: vi.fn().mockResolvedValue(body),
    } as unknown as Response;
}
