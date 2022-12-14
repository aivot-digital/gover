import {Preset} from '../models/preset';
import {CrudService} from './crud.service';

export const PresetsService = new CrudService<Preset, 'presets', number>('presets');
