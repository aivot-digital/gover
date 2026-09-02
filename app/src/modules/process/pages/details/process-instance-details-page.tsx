import {Box} from '@mui/material';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {SearchItemService} from '../../../search/search-item-service';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';

export function ProcessInstanceDetailsPage() {
    const {id} = useParams();

    useEffect(() => {
        if (id == null || id.length === 0) {
            return;
        }

        new SearchItemService()
            .recordRecentSearchItem({
                id,
                originTable: ServerEntityType.ProcessInstances,
            })
            .catch(() => {
            });
    }, [id]);

    return (
        <PageWrapper
            title="Vorgang"
            fullWidth={false}
            fullHeight={true}
        >
            <Box>
                Hier sind später die Details zu einem Vorgang zu finden.
            </Box>
        </PageWrapper>
    );
}
