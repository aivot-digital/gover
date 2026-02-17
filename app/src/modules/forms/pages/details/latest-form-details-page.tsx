import {useNavigate, useParams} from 'react-router-dom';
import {useEffect} from 'react';
import {FormVersionApiService} from '../../services/form-version-api-service';

export function LatestFormDetailsPage() {
    const formId = useParams().id;
    const navigate = useNavigate();

    useEffect(() => {
        if (formId == null) {
            return;
        }

        const formIdNum = parseInt(formId);

        new FormVersionApiService()
            .retrieve({
                formId: formIdNum,
                version: 'latest',
            })
            .then(res => {
                navigate(`/forms/${res.formId}/${res.version}`, {
                    replace: true,
                });
            });
    }, [formId]);

    return null;
}