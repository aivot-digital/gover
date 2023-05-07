import {DataOverview} from '../data-overview/data-overview';
import {DataOverviewProps} from '../data-overview/data-overview-props';
import {PresetsService} from '../../../services/presets.service';
import {Preset} from '../../../models/entities/preset';
import {ElementType} from '../../../data/element-type/element-type';
import strings from './presets-overview-strings.json';
import {Localization} from '../../../locale/localization';
import {generateElementIdForType} from "../../../utils/id-utils";
import ProjectPackage from '../../../../package.json';

const _ = Localization(strings);

const PresetsOverviewConfig: DataOverviewProps<Preset> = {
    title: _.title,
    addLabel: _.addLabel,
    emptySearchHelperText: _.emptySearchHelperText,
    noItemsHelperText: _.noItemsHelperText,
    searchPlaceholder: _.searchPlaceholder,

    exportExtension: 'prsts',

    list: () => {
        return PresetsService
            .list()
            .then(response => response._embedded.presets);
    },

    create: () => {
        const id = generateElementIdForType(ElementType.Container);
        return PresetsService
            .create({
                root: {
                    id: id,
                    type: ElementType.Container,
                    appVersion: ProjectPackage.version,
                    name: _.format(_.newPresetName, {id}),
                    children: [],
                },
            });
    },

    import: (preset) => {
        return PresetsService
            .create(preset);
    },

    update: (dest) => {
        return PresetsService
            .update(dest.id, dest);
    },

    destroy: (destroyed) => {
        return PresetsService.destroy(destroyed.id);
    },

    search: (search, item) => {
        return (item.root.name ?? '').toLowerCase().includes(search);
    },
    sort: (itemA, itemB) => (itemA.root.name ?? '').localeCompare(itemB.root.name ?? ''),

    toPrimaryString: preset => preset.root.name ?? '',

    linkToEdit: (preset: Preset) => '/presets/edit/' + preset.id,
};

export const PresetsOverview = () => (
    <DataOverview {...PresetsOverviewConfig}/>
);
