create or replace function jsonb_array_element_has_value(
    jsonb,
    text,
    text
) returns boolean as
$$
(select exists (select 1
                from jsonb_array_elements($1) as elem
                where elem ->> $2 = $3))
$$ language sql;