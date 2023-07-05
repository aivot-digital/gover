import {Preset} from '../models/entities/preset';
import {ApiService} from "./api-service";

export const PresetsService = new ApiService<Preset, Preset, number>('presets');
