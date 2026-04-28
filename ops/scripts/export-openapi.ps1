param(
  [string]$Output = "docs/openapi/openapi.json"
)
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -OutFile $Output
