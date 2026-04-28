param(
  [string]$Revision = "1",
  [string]$Namespace = "low-altitude-staging"
)
helm rollback low-altitude-rest-stop $Revision -n $Namespace
kubectl rollout status deployment/low-altitude-rest-stop-server -n $Namespace --timeout=180s
