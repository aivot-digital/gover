-- create a table for deputies for users.
-- a deputy consists of the id of the original user and the id of the user whom is a deputy for this ist.
-- a deputy can be limited in time but may be unlimited if the until date is null.
-- a user can have only one deputy in a given time range.
-- a user can have multiple deputies assigned for different time ranges.
-- a user can also be deputy for multiple users.
-- deputies are assigned and valid per timestamp ranges.
-- if a deputy assignment has no until timestamp, the assignment is valid from the from_timestamp onwards.
-- if a deputy assignment has an until timestamp, the assignment is valid from the from_timestamp until the until_timestamp.
create table user_deputies
(
    id               serial      not null primary key,
    original_user_id varchar(36) not null references users (id),
    deputy_user_id   varchar(36) not null references users (id),
    from_timestamp   timestamp   not null,
    until_timestamp  timestamp   null,
    -- prevent a user from being their own deputy
    check (original_user_id <> deputy_user_id),
    -- ensure that from_timestamp is before until_timestamp if until_timestamp is not null
    check (until_timestamp is null or from_timestamp < until_timestamp)
);

create view v_user_is_recursively_deputy_for as
with recursive deputy_hierarchy as (
    -- anchor member: direct deputies
    select ud.original_user_id as original_user_id,
           ud.deputy_user_id   as deputy_user_id,
           1                   as depth
    from user_deputies ud
    where (ud.until_timestamp is null or ud.until_timestamp > now())
      and ud.from_timestamp < now()

    union all

    -- recursive member: find deputies of deputies
    select dh.original_user_id,
           ud.deputy_user_id as deputy_user_id,
           dh.depth + 1      as depth
    from user_deputies ud
             inner join deputy_hierarchy dh on ud.original_user_id = dh.deputy_user_id
    where (ud.until_timestamp is null or ud.until_timestamp > now())
      and ud.from_timestamp < now())
select distinct dh.deputy_user_id,
                dh.original_user_id,
                dh.depth
from deputy_hierarchy dh;

-- create function to check if a deputy creates a circular reference
create or replace function check_deputy_circular_reference()
    returns trigger as
$$
begin
    if exists (select 1
               from v_user_is_recursively_deputy_for vh
               where vh.deputy_user_id = NEW.original_user_id
                 and vh.original_user_id = NEW.deputy_user_id) then
        raise exception 'Circular deputy assignment detected between users % and %', NEW.original_user_id, NEW.deputy_user_id;
    end if;
    return NEW;
end;
$$ language plpgsql;

-- create trigger to invoke the function before inserting a new deputy assignment
create trigger trg_check_deputy_circular_reference
    before insert
    on user_deputies
    for each row
execute function check_deputy_circular_reference();