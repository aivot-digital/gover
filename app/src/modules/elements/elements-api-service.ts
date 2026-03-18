import {Api} from '../../hooks/use-api';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';

interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

interface ElementDerivationRequest {
    element: AnyElement;
    authoredElementValues: AuthoredElementValues;
    derivationOptions: ElementDerivationOptions;
}

export class ElementsApiService {
    private readonly api: Api;

    public constructor(api: Api) {
        this.api = api;
    }

    public async derive(request: ElementDerivationRequest): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>('elements/derive/', request);
    }
}
