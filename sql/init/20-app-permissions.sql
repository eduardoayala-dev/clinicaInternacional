-- Solo inicialización de un volumen NUEVO de Docker Compose.
-- La imagen crea clinica_app con MYSQL_PASSWORD antes de ejecutar este script.
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'clinica_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON clinica_db.* TO 'clinica_app'@'%';
ALTER USER 'clinica_app'@'%' REQUIRE SSL;
