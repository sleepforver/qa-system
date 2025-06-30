create table conversation
(
    id         int auto_increment
        primary key,
    user_id    bigint       null,
    name       varchar(255) null,
    start_time datetime     null
);

create table users
(
    id         bigint auto_increment
        primary key,
    username   varchar(50)                         not null,
    password   varchar(100)                        not null,
    email      varchar(100)                        not null,
    created_at timestamp default CURRENT_TIMESTAMP null,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

create table chat_history
(
    id              bigint auto_increment
        primary key,
    user_id         bigint                              not null,
    user_message    text                                not null,
    ai_response     text                                null,
    version         int                                 null comment '版本',
    created_at      timestamp default CURRENT_TIMESTAMP null,
    conversation_id int                                 null,
    constraint chat_history_ibfk_1
        foreign key (user_id) references users (id)
);

create index user_id
    on chat_history (user_id);

