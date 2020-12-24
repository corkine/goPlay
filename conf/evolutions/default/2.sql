#--- Create Entity
#--- !Ups
create table IF NOT EXISTS "EntityLogs" (
                                            "entityId" BIGINT,
                                            "IPAddress" VARCHAR NOT NULL,
                                            "action" VARCHAR NOT NULL,
                                            "time" TIMESTAMP NOT NULL,
                                            "id" BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT);

alter table "EntityLogs"
    add constraint "entity_fk" foreign key("entityId")
        references "Entities"("id") on update RESTRICT on delete SET NULL;


#--- !Downs
drop table if exists EntityLogs;