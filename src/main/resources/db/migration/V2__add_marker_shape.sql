-- Add a shape (ESRI simple-marker style) to each marker. Existing rows default
-- to CIRCLE, matching the previous hard-coded circle symbol. Stored as the enum
-- name (see MarkerShape / @Enumerated(STRING)).
alter table marker_symbol
    add column shape varchar(16) not null default 'CIRCLE';
