/** 天气可视化工具：提供天气图标映射、雷暴风险颜色和天气描述格式化，与 Android 端 WeatherVisuals 逻辑对齐 */

export type WeatherIconType = 'clear' | 'cloudy' | 'rain' | 'snow' | 'fog' | 'thunderstorm'

const WEATHER_ICON_MAP: Record<WeatherIconType, string> = {
  clear: '☀️',
  cloudy: '⛅',
  rain: '🌧️',
  snow: '❄️',
  fog: '🌫️',
  thunderstorm: '⛈️',
}

const WEATHER_BG_MAP: Record<WeatherIconType, string> = {
  clear: 'linear-gradient(135deg, #fef9c3 0%, #fde68a 100%)',
  cloudy: 'linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%)',
  rain: 'linear-gradient(135deg, #dbeafe 0%, #93c5fd 100%)',
  snow: 'linear-gradient(135deg, #f0f9ff 0%, #bae6fd 100%)',
  fog: 'linear-gradient(135deg, #f5f5f4 0%, #d6d3d1 100%)',
  thunderstorm: 'linear-gradient(135deg, #312e81 0%, #4c1d95 100%)',
}

const WEATHER_TEXT_COLOR_MAP: Record<WeatherIconType, string> = {
  clear: '#92400e',
  cloudy: '#3730a3',
  rain: '#1e40af',
  snow: '#0c4a6e',
  fog: '#57534e',
  thunderstorm: '#e0e7ff',
}

const RISK_LEVEL_LABELS = ['极低', '很低', '偏低', '中等', '偏高', '高', '很高', '严重', '极高']

const RISK_COLOR_MAP: Record<string, string> = {
  '1': '#22c55e',
  '2': '#4ade80',
  '3': '#a3e635',
  '4': '#facc15',
  '5': '#fb923c',
  '6': '#f97316',
  '7': '#ef4444',
  '8': '#dc2626',
  '9': '#991b1b',
}

export function resolveWeatherIconType(weather: string): WeatherIconType {
  if (!weather) return 'clear'
  if (weather.includes('雷')) return 'thunderstorm'
  if (weather.includes('雨')) return 'rain'
  if (weather.includes('雪')) return 'snow'
  if (weather.includes('雾') || weather.includes('霾')) return 'fog'
  if (weather.includes('阴') || weather.includes('云')) return 'cloudy'
  return 'clear'
}

export function resolveWeatherEmoji(iconType: WeatherIconType): string {
  return WEATHER_ICON_MAP[iconType] ?? '🌤'
}

export function resolveWeatherBackground(iconType: WeatherIconType): string {
  return WEATHER_BG_MAP[iconType] ?? WEATHER_BG_MAP.clear
}

export function resolveWeatherTextColor(iconType: WeatherIconType): string {
  return WEATHER_TEXT_COLOR_MAP[iconType] ?? WEATHER_TEXT_COLOR_MAP.clear
}

export function resolveRiskColor(level: number): string {
  const clamped = Math.max(1, Math.min(9, level))
  return RISK_COLOR_MAP[String(clamped)] ?? '#64748b'
}

export function resolveRiskLabel(level: number): string {
  const clamped = Math.max(1, Math.min(9, level))
  return RISK_LEVEL_LABELS[clamped - 1] ?? '未知'
}

export function formatRiskSummary(level: number, label: string): string {
  return `${Math.max(1, Math.min(9, level))}级 ${label}`
}
