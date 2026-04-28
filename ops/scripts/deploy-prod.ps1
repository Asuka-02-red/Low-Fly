param(
  [string]$ImageTag
)
if (-not $ImageTag) { throw "必须提供 ImageTag" }
helm upgrade --install low-altitude-rest-stop "$PSScriptRoot\..\helm\low-altitude-rest-stop" --namespace low-altitude-prod --create-namespace -f "$PSScriptRoot\..\helm\low-altitude-rest-stop\values.yaml" -f "$PSScriptRoot\..\helm\low-altitude-rest-stop\values-prod.yaml" --set image.tag=$ImageTag
kubectl rollout status deployment/low-altitude-rest-stop-server -n low-altitude-prod --timeout=180s
