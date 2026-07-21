-- Initial schema for the marker_symbol table.
--
-- Mirrors the com.appmcore.mapapp.entity.MarkerSymbol mapping so that the
-- staging/prod profiles (ddl-auto: validate) start against a fresh PostgreSQL
-- database. Column types match Hibernate's generated DDL exactly so schema
-- validation passes.
create table marker_symbol (
    id         uuid                        not null,
    latitude   numeric(9, 6)               not null,
    longitude  numeric(9, 6)               not null,
    label      varchar(255),
    color      varchar(7)                  not null,
    size       integer                     not null,
    created_at timestamp(6) with time zone not null,
    primary key (id)
);
