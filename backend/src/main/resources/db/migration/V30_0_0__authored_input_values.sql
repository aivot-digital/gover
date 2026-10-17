-- Authored element values use an explicit input-mode envelope from this version onward. Replicating-container rows
-- contain another authored map below `values`, so those maps must be migrated recursively as well.
create function migrate_authored_literal_value(input_value jsonb)
    returns jsonb
    language plpgsql
    immutable
as
$$
declare
    migrated_value jsonb;
begin
    if jsonb_typeof(input_value) = 'array' then
        select coalesce(
                       jsonb_agg(
                               case
                                   -- Restrict row detection to the canonical `{id, values}` shape so ordinary
                                   -- business objects with a property named `values` remain untouched.
                                   when jsonb_typeof(item.value) = 'object'
                                       and item.value ? 'values'
                                       and jsonb_typeof(item.value -> 'values') = 'object'
                                       and not exists(
                                           select 1
                                           from jsonb_object_keys(item.value) as row_key(key)
                                           where row_key.key not in ('id', 'values')
                                       )
                                       then (item.value - 'values') || jsonb_build_object(
                                           'values',
                                           coalesce(
                                               (
                                                   select jsonb_object_agg(
                                                           row_entry.key,
                                                           jsonb_build_object(
                                                               'type', 'Literal',
                                                               'value', migrate_authored_literal_value(row_entry.value)
                                                           )
                                                       )
                                                   from jsonb_each(item.value -> 'values') as row_entry
                                               ),
                                               '{}'::jsonb
                                           )
                                       )
                                   else migrate_authored_literal_value(item.value)
                                   end
                               order by item.position
                       ),
                       '[]'::jsonb
               )
        into migrated_value
        from jsonb_array_elements(input_value) with ordinality as item(value, position);
        return migrated_value;
    end if;

    if jsonb_typeof(input_value) = 'object' then
        select coalesce(
                       jsonb_object_agg(entry.key, migrate_authored_literal_value(entry.value)),
                       '{}'::jsonb
               )
        into migrated_value
        from jsonb_each(input_value) as entry;
        return migrated_value;
    end if;

    return input_value;
end;
$$;

update payment_providers
set config = coalesce(
        (
            select jsonb_object_agg(
                    entry.key,
                    jsonb_build_object('type', 'Literal', 'value', migrate_authored_literal_value(entry.value))
                )
            from jsonb_each(payment_providers.config) as entry
        ),
        '{}'::jsonb
    );

update storage_providers
set configuration = coalesce(
        (
            select jsonb_object_agg(
                    entry.key,
                    jsonb_build_object('type', 'Literal', 'value', migrate_authored_literal_value(entry.value))
                )
            from jsonb_each(storage_providers.configuration) as entry
        ),
        '{}'::jsonb
    );

update process_nodes
set configuration = coalesce(
        (
            select jsonb_object_agg(
                    entry.key,
                    jsonb_build_object('type', 'Literal', 'value', migrate_authored_literal_value(entry.value))
                )
            from jsonb_each(process_nodes.configuration) as entry
        ),
        '{}'::jsonb
    );

drop function migrate_authored_literal_value(jsonb);
