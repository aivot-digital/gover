-- create function for array intersection
create function array_intersect(varchar(32)[], varchar(32)[])
    returns varchar(32)[] AS
$$
select array(
           -- UNNEST converts array to a set of rows (elements)
               select unnest($1)
               intersect
               select unnest($2)
               -- Add ORDER BY to guarantee a consistent array order, though it's optional
               order by 1
       );
$$ language sql immutable;

-- create aggregate function for array intersection
create aggregate array_intersect_agg(varchar(32)[]) (
    sfunc = array_intersect,
    stype = varchar(32)[],
    -- Initial condition should be NULL so the first array becomes the initial state
    initcond = '{}'
    );

-- create function for array unique union
create function array_unique_union(varchar(32)[], varchar(32)[])
    returns varchar(32)[] AS
$$
select array(
               select unnest($1)
               union
               select unnest($2)
               order by 1 -- Optional: ensures a consistent order
       );
$$ language sql immutable;

-- create aggregate function for array unique union
create aggregate array_unique_union_agg(varchar(32)[]) (
    sfunc = array_unique_union,
    stype = varchar(32)[],
    -- INITCOND is NULL, meaning the first array becomes the initial state
    initcond = '{}'
    );