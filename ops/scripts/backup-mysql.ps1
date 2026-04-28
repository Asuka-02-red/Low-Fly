param(
  [string]$OutputFile = "backup.sql"
)
docker exec low-altitude-mysql mysqldump -uroot -proot low_altitude_rest_stop > $OutputFile
