alter table document_chunk add column document_id varchar(80);
alter table document_chunk add column chunk_index integer;
alter table document_chunk add column page_number integer;
alter table document_chunk add column start_offset integer;
alter table document_chunk add column end_offset integer;
alter table document_chunk add column source_type varchar(40);
alter table document_chunk add column source_name varchar(255);

create index idx_document_chunk_document_id on document_chunk(document_id);
create index idx_document_chunk_user_document_chunk on document_chunk(user_id, document_id, chunk_index);
