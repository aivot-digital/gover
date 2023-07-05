import {ApiService} from "./api-service";
import {DepartmentMembership} from "../models/entities/department-membership";


export const DepartmentMembershipsService = new ApiService<DepartmentMembership, DepartmentMembership, number>('department-memberships');
