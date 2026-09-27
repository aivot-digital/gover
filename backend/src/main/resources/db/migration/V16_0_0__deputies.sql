-- create a table for deputies for users.
-- a deputy consists of the id of the original user and the id of the user whom is a deputy for this ist.
-- a deputy can be limited in time but may be unlimited if the until date is null.
-- a user can have only one deputy in a given time range.
-- a user can have multiple deputies assigned for different time ranges.
-- a user can also be deputy for multiple users.
-- deputies are assigned and valid for inclusive date ranges.
-- if a deputy assignment has no until date, the assignment is valid from the from_date onwards.
-- if a deputy assignment has an until date, the assignment is valid from from_date through until_date.
create table user_deputies
(
    id               serial      not null primary key,
    original_user_id varchar(36) not null references users (id),
    deputy_user_id   varchar(36) not null references users (id),
    from_date        date        not null,
    until_date       date        null,
    -- prevent a user from being their own deputy
    check (original_user_id <> deputy_user_id),
    -- single-day assignments are valid, so both boundaries are inclusive
    check (until_date is null or from_date <= until_date)
);

create view v_user_is_recursively_deputy_for as
with recursive deputy_hierarchy as (
    -- anchor member: direct deputies
    -- current_date follows the application timezone configured for each database connection
    select ud.original_user_id as original_user_id,
           ud.deputy_user_id   as deputy_user_id,
           1                   as depth
    from user_deputies ud
    where ud.from_date <= current_date
      and (ud.until_date is null or current_date <= ud.until_date)

    union all

    -- recursive member: find deputies of deputies
    select dh.original_user_id,
           ud.deputy_user_id as deputy_user_id,
           dh.depth + 1      as depth
    from user_deputies ud
             inner join deputy_hierarchy dh on ud.original_user_id = dh.deputy_user_id
    where ud.from_date <= current_date
      and (ud.until_date is null or current_date <= ud.until_date))
select distinct dh.deputy_user_id,
                dh.original_user_id,
                dh.depth
from deputy_hierarchy dh;

-- create function to check if a deputy creates a circular reference
create or replace function fun_check_deputy_circular_reference()
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
execute function fun_check_deputy_circular_reference();
