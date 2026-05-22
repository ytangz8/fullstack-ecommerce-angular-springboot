-- Countries
INSERT INTO country (code, name) VALUES ('US', 'United States');
INSERT INTO country (code, name) VALUES ('CA', 'Canada');
INSERT INTO country (code, name) VALUES ('IN', 'India');
INSERT INTO country (code, name) VALUES ('BR', 'Brazil');

-- States: United States
INSERT INTO state (name, country_id) VALUES ('Alabama', 1);
INSERT INTO state (name, country_id) VALUES ('Alaska', 1);
INSERT INTO state (name, country_id) VALUES ('Arizona', 1);
INSERT INTO state (name, country_id) VALUES ('California', 1);
INSERT INTO state (name, country_id) VALUES ('Colorado', 1);
INSERT INTO state (name, country_id) VALUES ('Florida', 1);
INSERT INTO state (name, country_id) VALUES ('Georgia', 1);
INSERT INTO state (name, country_id) VALUES ('New York', 1);
INSERT INTO state (name, country_id) VALUES ('Texas', 1);
INSERT INTO state (name, country_id) VALUES ('Washington', 1);

-- States: Canada
INSERT INTO state (name, country_id) VALUES ('Alberta', 2);
INSERT INTO state (name, country_id) VALUES ('British Columbia', 2);
INSERT INTO state (name, country_id) VALUES ('Ontario', 2);
INSERT INTO state (name, country_id) VALUES ('Quebec', 2);

-- States: India
INSERT INTO state (name, country_id) VALUES ('Maharashtra', 3);
INSERT INTO state (name, country_id) VALUES ('Karnataka', 3);
INSERT INTO state (name, country_id) VALUES ('Tamil Nadu', 3);
INSERT INTO state (name, country_id) VALUES ('Delhi', 3);

-- States: Brazil
INSERT INTO state (name, country_id) VALUES ('Sao Paulo', 4);
INSERT INTO state (name, country_id) VALUES ('Rio de Janeiro', 4);
INSERT INTO state (name, country_id) VALUES ('Brasilia', 4);
