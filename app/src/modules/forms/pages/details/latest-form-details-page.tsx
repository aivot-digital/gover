import {useNavigate, useParams} from 'react-router-dom';
import {useEffect} from 'react';
import {FormsApiService} from '../../forms-api-service-v2';

export function LatestFormDetailsPage() {
    const formId = useParams().id;
    const navigate = useNavigate();

    useEffect(() => {
        if (formId == null) {
            return;
        }

        const formIdNum = parseInt(formId);

        new FormsApiService()
            .retrieveLatest(formIdNum)
            .then(res => {
                navigate(`/forms/${res.id}/${res.version}`, {
                    replace: true,
                });
            })
    }, [formId]);

    return null;
}