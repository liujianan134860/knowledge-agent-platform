create table refresh_token (
    token_hash varchar(255) not null,
    user_id varchar(64) not null,
    created_at timestamp not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    primary key (token_hash)
);

create table revoked_access_token (
    token_id varchar(255) not null,
    user_id varchar(64) not null,
    revoked_at timestamp not null,
    primary key (token_id)
);

create index idx_refresh_token_user_active on refresh_token(user_id, revoked_at, expires_at);
create index idx_revoked_access_token_user_revoked on revoked_access_token(user_id, revoked_at);
