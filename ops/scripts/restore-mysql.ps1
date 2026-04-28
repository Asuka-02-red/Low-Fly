param(
  [string]$InputFile = "backup.sql"
)
Get-Content $InputFile | docker exec -i low-altitude-mysql mysql -uroot -proot low_altitude_rest_stop
