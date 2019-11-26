CREATE TABLE IF NOT EXISTS Songs (
id BIGINT auto_increment,
artist VARCHAR(255),
year VARCHAR(255),
album VARCHAR(255),
title VARCHAR(255)
);

DELETE FROM Songs;