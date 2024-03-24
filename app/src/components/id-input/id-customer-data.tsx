import {AuthDataDto} from '../../models/dtos/auth-data-dto';

export interface IdCustomerData {
    authData: AuthDataDto;
    userInfo: any;
    idp: string;
}