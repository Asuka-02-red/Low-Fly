param(
  [string]$ImageTag = "latest"
)
helm upgrade --install low-altitude-rest-stop "$PSScriptRoot\..\helm\low-altitude-rest-stop" --namespace low-altitude-staging --create-namespace -f "$PSScriptRoot\..\helm\low-altitude-rest-stop\values.yaml" -f "$PSScriptRoot\..\helm\low-altitude-rest-stop\values-staging.yaml" --set image.tag=$ImageTag
kubectl rollout status deployment/low-altitude-rest-stop-server -n low-altitude-staging --timeout=180s
