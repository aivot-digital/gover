import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {UserConfigRequestDto} from './dtos/user-config-request-dto';
import {UserConfigResponseDto} from './dtos/user-config-response-dto';
import {UserConfigDefinitionResponseDTO} from './dtos/user-config-definition-response-dto';

interface UserConfigsFilter {
    publicConfig: boolean;
}

export class UserConfigsApiService extends CrudApiService<UserConfigRequestDto, UserConfigResponseDto, UserConfigResponseDto, UserConfigResponseDto, UserConfigResponseDto, string, UserConfigsFilter> {
    public constructor(api: Api, userId: string) {
        super(api, `user-configs/${userId}/`);
    }

    public initialize(): UserConfigResponseDto {
        return {
            key: '',
            value: '',
            userId: '',
            publicConfig: false,
        };
    }

    public async listDefinitions(): Promise<UserConfigDefinitionResponseDTO[]> {
        return await this.api.get<UserConfigDefinitionResponseDTO[]>(`user-config-definitions/`);
    }
}