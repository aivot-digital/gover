-- Code list columns are referenced through stable keys instead of their position, so a source that reorders its
-- columns no longer changes which cells a reference reads. Value and label columns are chosen per element.
drop view if exists v_code_list_items;

alter table code_lists
    add column if not exists column_mappings jsonb default '[]'::jsonb not null,
    add column if not exists key_column_key  varchar(96);

-- Every existing column becomes a mapping whose key is the source column name. Duplicate or blank names receive a
-- positional key, so all keys of a list are unique.
with column_names as (select cl.id,
                             c.position,
                             coalesce(nullif(trim(c.name), ''), 'COL' || (c.position - 1)) as name
                      from code_lists cl
                               cross join lateral unnest(cl.columns) with ordinality as c(name, position)),
     column_keys as (select id,
                            position,
                            name,
                            case
                                when row_number() over (partition by id, name order by position) = 1 then name
                                else name || '_' || position
                                end as key
                     from column_names)
update code_lists cl
set column_mappings = mappings.value
from (select id,
             jsonb_agg(jsonb_build_object('key', key, 'name', name, 'index', position) order by position) as value
      from column_keys
      group by id) mappings
where cl.id = mappings.id
  and cl.column_mappings = '[]'::jsonb;

-- The former value column identifies the rows, e.g. when outdated rows are kept during a synchronization.
update code_lists cl
set key_column_key = (select mapping ->> 'key'
                      from jsonb_array_elements(cl.column_mappings) mapping
                      where (mapping ->> 'index')::integer = cl.value_column_index + 1)
where cl.key_column_key is null;

alter table code_lists
    drop column if exists value_column_index,
    drop column if exists label_column_index;
