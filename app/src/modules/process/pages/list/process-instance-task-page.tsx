import {useParams} from 'react-router-dom';
import {ProcessTaskList} from './process-task-list';

export function ProcessInstanceTaskListPage() {
    const {instanceId} = useParams();
    return <ProcessTaskList instanceId={Number(instanceId)} />;
}
