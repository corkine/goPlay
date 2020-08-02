# --- Create Entity
# --- !Ups
create table IF NOT EXISTS `Entities` (
    `keyword` VARCHAR NOT NULL,
    `redirectURL` VARCHAR NOT NULL,
    `note` VARCHAR,
    `updateTime` TIMESTAMP NOT NULL,
    `id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT);

create table IF NOT EXISTS `Users` (
    `username` VARCHAR NOT NULL,
    `password` VARCHAR NOT NULL,
    `userType` VARCHAR NOT NULL,
    `id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT);

insert into "Users" ("username", "password", "userType")
values ('corkine', 'mi960032', 'Admin');

# --- !Downs
drop table if exists Entities;
drop table if exists Users;