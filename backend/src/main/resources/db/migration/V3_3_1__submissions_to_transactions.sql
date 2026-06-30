-- Create transactions for submissions with payment requests

insert into payment_transactions (key, payment_provider_key, payment_request, payment_information, payment_error, redirect_url, created, updated)
select id                                                        as key,
       (select key
        from payment_providers
        where provider_key = 'epaybl'
          and config ->> 'endpointId' = payment_endpoint_id
          and config ->> 'originatorId' = payment_originator_id) as payment_provider_key,
       payment_request                                           as payment_request,
       payment_information                                       as payment_information,
       payment_error                                             as payment_error,
       '/' || slug || '/' || version || '?submissionId' || id    as redirect_url,
       now()                                                     as created,
       now()                                                     as updated
from (select submissions.*,
             forms.slug,
             forms.version
      from submissions
               join forms on submissions.form_id = forms.id) as submissions
where payment_request is not null
  and payment_provider = 'epaybl';

insert into payment_transactions (key, payment_provider_key, payment_request, payment_information, payment_error, redirect_url, created, updated)
select id                                                        as key,
       (select key
        from payment_providers
        where provider_key = 'pmpayment'
          and config ->> 'endpointId' = payment_endpoint_id
          and config ->> 'originatorId' = payment_originator_id) as payment_provider_key,
       payment_request                                           as payment_request,
       payment_information                                       as payment_information,
       payment_error                                             as payment_error,
       '/' || slug || '/' || version || '?submissionId' || id    as redirect_url,
       now()                                                     as created,
       now()                                                     as updated
from (select submissions.*,
             forms.slug,
             forms.version
      from submissions
               join forms on submissions.form_id = forms.id) as submissions
where payment_request is not null
  and payment_provider = 'pmpayment';

insert into payment_transactions (key, payment_provider_key, payment_request, payment_information, payment_error, redirect_url, created, updated)
select id                                                                 as key,
       (select key
        from payment_providers
        where provider_key = 'giropay'
          and config ->> 'sellerId' = submissions.payment_originator_id
          and config ->> 'projectId' = submissions.payment_endpoint_id) as payment_provider_key,
       payment_request                                                    as payment_request,
       payment_information                                                as payment_information,
       payment_error                                                      as payment_error,
       '/' || slug || '/' || version || '?submissionId' || id             as redirect_url,
       now()                                                              as created,
       now()                                                              as updated
from (select submissions.*,
             forms.slug,
             forms.version
      from submissions
               join forms on submissions.form_id = forms.id) as submissions
where payment_request is not null
  and payment_provider = 'giropay';

-- Add new column to submissions table for transaction key

alter table submissions
    add column payment_transaction_key varchar(36) null references payment_transactions (key) on delete restrict;

-- Update all submissions with new references to the payment transactions

update submissions
set payment_transaction_key = id
where payment_request is not null;

-- Drop originatorId and endpointId columns from submissions table

alter table submissions
    drop column payment_originator_id,
    drop column payment_endpoint_id,
    drop column payment_provider,
    drop column payment_request,
    drop column payment_information,
    drop column payment_error;
