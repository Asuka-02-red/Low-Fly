/** 飞行适宜性评估器：根据气象数据推导飞行安全指标，与 Android 端 WeatherFlightEvaluator 逻辑对齐 */

export interface DerivedMetrics {
  windSpeed: number
  visibility: number
  precipitationProbability: number
  precipitationIntensity: number
  thunderstormRisk: string
  thunderstormRiskLevel: number
  thunderstormRiskLabel: string
  thunderstormRiskHint: string
  thunderstormProtectionAdvice: string
}

export interface FlightCheck {
  label: string
  currentValue: string
  threshold: string
  passed: boolean
}

export interface FlightSuitabilityResult {
  result: '适宜飞行' | '不适宜飞行'
  level: string
  summary: string
  checks: FlightCheck[]
  conditionNotes: string[]
  recommendations: string[]
}

const WIND_SPEED_LEVELS: Record<number, number> = {
  0: 0.2, 1: 1.5, 2: 3.3, 3: 5.4, 4: 7.9, 5: 10.7,
  6: 13.8, 7: 17.1, 8: 20.7, 9: 24.4, 10: 28.4, 11: 32.6, 12: 36.9,
}

const RISK_HINTS: Record<string, string> = {
  '1-2': '雷暴信号极弱，可继续例行监测。',
  '3-4': '存在轻微对流扰动，建议缩短观察周期并复核预报。',
  '5-6': '对流条件开始增强，任务执行前需结合空域和备降条件复核。',
  '7-8': '强对流触发概率较高，建议暂停起飞并将设备撤离暴露区域。',
  '9': '极端雷暴风险窗口，严禁起飞并立即执行人员与设备防护。',
}

const PROTECTION_ADVICE: Record<string, string> = {
  '1-2': '保持常规值守，继续关注近 30 分钟内雷达回波与云团发展。',
  '3-4': '缩短复测周期，起飞前再次确认风速和回波强度。',
  '5-6': '优先安排地面准备，必要时收紧飞行半径并准备备降点。',
  '7-8': '暂停飞行，人员和设备应远离空旷暴露区域并切断非必要作业。',
  '9': '立即停飞并执行防雷避险，全部设备应断电入库并等待风险解除。',
}

export function containsAny(text: string, ...keywords: string[]): boolean {
  for (const keyword of keywords) {
    if (text.includes(keyword)) return true
  }
  return false
}

export function parseWindSpeed(windPowerText: string): number {
  if (!windPowerText?.trim()) return 0.0
  const normalized = windPowerText.replace('≤', '').replace('级', '').trim()
  const segments = normalized.split('-')
  try {
    const level = parseInt(segments[segments.length - 1].trim(), 10)
    return WIND_SPEED_LEVELS[level] ?? 0.0
  } catch {
    return 0.0
  }
}

function round(value: number): number {
  return Math.round(value * 10) / 10
}

function buildRiskDescriptor(
  thunderstorm: boolean,
  windSpeed: number,
  visibility: number,
  precipitationProbability: number,
  precipitationIntensity: number,
): { level: number; label: string; hint: string; protectionAdvice: string } {
  let score = 1
  if (thunderstorm) score += 4
  if (precipitationProbability >= 90) score += 2
  else if (precipitationProbability >= 55) score += 1
  if (precipitationIntensity >= 2.0) score += 2
  else if (precipitationIntensity >= 0.5) score += 1
  if (visibility <= 2.0) score += 1
  else if (visibility <= 5.0) score += 1
  if (windSpeed >= 17.0) score += 2
  else if (windSpeed >= 10.0) score += 1

  const level = Math.max(1, Math.min(9, score))
  const label = resolveRiskLabel(level)
  return { level, label, hint: buildRiskHint(level), protectionAdvice: buildProtectionAdvice(level) }
}

function resolveRiskLabel(level: number): string {
  const labels = ['极低', '很低', '偏低', '中等', '偏高', '高', '很高', '严重', '极高']
  return labels[Math.max(0, Math.min(8, level - 1))]
}

function buildRiskHint(level: number): string {
  if (level <= 2) return RISK_HINTS['1-2']
  if (level <= 4) return RISK_HINTS['3-4']
  if (level <= 6) return RISK_HINTS['5-6']
  if (level <= 8) return RISK_HINTS['7-8']
  return RISK_HINTS['9']
}

function buildProtectionAdvice(level: number): string {
  if (level <= 2) return PROTECTION_ADVICE['1-2']
  if (level <= 4) return PROTECTION_ADVICE['3-4']
  if (level <= 6) return PROTECTION_ADVICE['5-6']
  if (level <= 8) return PROTECTION_ADVICE['7-8']
  return PROTECTION_ADVICE['9']
}

export function deriveFromServerData(
  windSpeed: number,
  visibility: number,
  precipitationProbability: number,
  precipitationIntensity: number,
  thunderstormRisk: string,
): DerivedMetrics {
  const thunderstorm = thunderstormRisk === '高'
  const descriptor = buildRiskDescriptor(thunderstorm, windSpeed, visibility, precipitationProbability, precipitationIntensity)
  return {
    windSpeed: round(windSpeed),
    visibility: round(visibility),
    precipitationProbability,
    precipitationIntensity: round(precipitationIntensity),
    thunderstormRisk,
    thunderstormRiskLevel: descriptor.level,
    thunderstormRiskLabel: descriptor.label,
    thunderstormRiskHint: descriptor.hint,
    thunderstormProtectionAdvice: descriptor.protectionAdvice,
  }
}

export function buildSuitabilityView(metrics: DerivedMetrics): FlightSuitabilityResult {
  const windOk = metrics.windSpeed < 10.0
  const visibilityOk = metrics.visibility > 5.0
  const precipitationOk = metrics.precipitationIntensity < 0.5
  const thunderstormOk = metrics.thunderstormRiskLevel <= 3
  const suitable = windOk && visibilityOk && precipitationOk && thunderstormOk

  const checks: FlightCheck[] = [
    { label: '风速', currentValue: `${metrics.windSpeed.toFixed(1)}m/s`, threshold: '< 10m/s', passed: windOk },
    { label: '能见度', currentValue: `${metrics.visibility.toFixed(1)}km`, threshold: '> 5km', passed: visibilityOk },
    { label: '降水强度', currentValue: `${metrics.precipitationIntensity.toFixed(1)}mm/h`, threshold: '< 0.5mm/h', passed: precipitationOk },
    { label: '雷暴风险', currentValue: `${metrics.thunderstormRiskLevel}级 ${metrics.thunderstormRisk}`, threshold: '1-3级', passed: thunderstormOk },
  ]

  const conditionNotes = [
    windOk ? '风速处于安全阈值内。' : '当前风速超过 10m/s，存在姿态控制风险。',
    visibilityOk ? '能见度满足基本目视飞行要求。' : '能见度低于 5km，不满足稳妥起飞条件。',
    precipitationOk ? '降水强度较低，对起降影响可控。' : '降水强度高于 0.5mm/h，建议暂停任务。',
    thunderstormOk ? '雷暴等级处于低风险窗口，可继续观测云团变化。' : metrics.thunderstormRiskHint,
  ]

  const recommendations = suitable
    ? [
        '当前气象窗口可执行常规飞行任务，建议起飞前复核空域审批与电池状态。',
        '持续关注首页天气模块，系统会每 15 分钟自动刷新一次气象判断。',
      ]
    : [
        windOk ? '保持低空待命并继续观测近地风变化。' : '建议等待风速回落到 10m/s 以下后再评估起飞。',
        visibilityOk ? '保持视距飞行边界，不建议扩大作业半径。' : '建议延后任务，待能见度恢复至 5km 以上。',
        precipitationOk ? '如需紧急作业，请同步确认机体防水等级。' : '建议暂停飞行，避免降水影响机体与图传链路。',
        thunderstormOk ? '继续关注云团与地面站告警。' : metrics.thunderstormProtectionAdvice,
      ]

  return {
    result: suitable ? '适宜飞行' : '不适宜飞行',
    level: suitable ? '绿色窗口' : '风险预警',
    summary: suitable
      ? '风速、能见度、降水强度与雷暴风险均满足当前飞行安全阈值。'
      : '至少一项关键气象指标超出飞行安全阈值，当前不建议执行飞行任务。',
    checks,
    conditionNotes,
    recommendations,
  }
}
