# Network Monitor Environment

The project environment is configured with:

- Temurin OpenJDK 21.0.11 at `../jdk/jdk-21.0.11+10`
- MySQL Community Server 8.4.9
- MySQL Connector/J 8.4.0
- JFreeChart 1.5.6 and JCommon 1.0.24
- Eclipse classpath metadata in `.classpath`

## Database

The local development server uses `127.0.0.1:3306` and the `root` account with no password. The database `network_monitor_db` has already been created from `Documents/schema.sql`.

To start it again after a restart:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --basedir='C:\Program Files\MySQL\MySQL Server 8.4' --datadir="$PWD\mysql-data" --port=3306 --bind-address=127.0.0.1 --console
```

To rebuild the schema:

```powershell
Get-Content Documents\schema.sql -Raw | & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' --protocol=tcp -h 127.0.0.1 -P 3306 -u root
```

For a production or shared machine, set a root password and move the data directory outside the project folder.