# --- Create Entity
# --- !Ups
create table Entity(id bigint not null auto_increment primary key ,
                    name varchar(255) not null,
                    shortUrl varchar(255) not null,
                    longUrl varchar(255) not null);
;
# --- !Downs
drop table if exists Entity;