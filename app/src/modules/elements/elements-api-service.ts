import {Api} from '../../hooks/use-api';
import {ElementData} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';

interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

interface ElementDerivationRequest {
    element: AnyElement;
    elementData: ElementData;
    options: ElementDerivationOptions;
}

export class ElementsApiService {
    private readonly api: Api;

    public constructor(api: Api) {
        this.api = api;
    }

    public async derive(request: ElementDerivationRequest): Promise<ElementData> {
        return await this.api.post<ElementData>('elements/derive/', request);
    }
}