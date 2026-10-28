-- Vistas que siante2 lee hoy. Definiciones INFERIDAS de sus columnas y del legacy (docs/modelo-datos.md):
-- reemplazarlas por las de Oracle cuando el DBA las entregue.

create view v_layers as
select l.id, l.name, l.origin, l.json_metadata, l.description, l.visibility, l.status,
       u.id as user_id, u.name as user_name, u.last_name as user_last_name, u.email as user_email,
       u.area as user_area, c.id as category_id, c.name as category_name
from layers l
left join users u on u.id = l.owner
left join categories c on c.id = l.category_id;

create view v_layers_groups as
select g.id, g.name, g.description, g.owner, g.status, g.is_scheduler, g.create_type,
       (select count(*) from layers_groups_layers gl where gl.l_group_id = g.id)::integer as layers_count
from layers_groups g;

-- Correlativos de reportes públicos: cada lectura entrega el siguiente, como NEXTVAL_* en Oracle. Usan la
-- misma secuencia que la columna identity, así que un alta con o sin el correlativo no choca.
create view nextval_public_reports as
select nextval(pg_get_serial_sequence('public_reports', 'x_pure'))::integer as correlativo;

create view nextval_public_reports_details as
select nextval(pg_get_serial_sequence('public_reports_details', 'x_puredet'))::integer as correlativo;
