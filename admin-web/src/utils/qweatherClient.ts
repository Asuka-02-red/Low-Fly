/** 和风天气API客户端，直接调用和风天气开放接口获取实时天气数据，与 Android 端 QWeatherClient.java 完全对齐。 */
import type { RealtimeWeatherPayload } from '@/types'
import { resolveWeatherIconType } from '@/utils/weatherVisuals'
import { deriveFromServerData, buildSuitabilityView } from '@/utils/weatherFlightEvaluator'

const WIND_SPEED_LEVELS: Record<number, number> = {
    0: 0.2, 1: 1.5, 2: 3.3, 3: 5.4, 4: 7.9, 5: 10.7,
    6: 13.8, 7: 17.1, 8: 20.7, 9: 24.4, 10: 28.4, 11: 32.6, 12: 36.9,
}

const QWEATHER_API_KEY = 'ef1a1c145b2e41b68c278e62d832c4a2'
const QWEATHER_API_HOST = 'https://mm2pg8ecbq.re.qweatherapi.com'
const CALL_TIMEOUT_MS = 10000

export class QWeatherClient {
    private readonly apiKey: string
    private readonly apiHost: string

    constructor(apiKey = QWEATHER_API_KEY, apiHost = QWEATHER_API_HOST) {
        this.apiKey = apiKey
        this.apiHost = apiHost
    }

    isConfigured(): boolean {
        return (
            this.apiKey != null &&
            this.apiKey.trim().length > 0 &&
            this.apiHost != null &&
            this.apiHost.trim().length > 0
        )
    }

    async fetchRealtimeWeather(
        longitude: number,
        latitude: number,
    ): Promise<RealtimeWeatherPayload | null> {
        if (!this.isConfigured()) {
            console.warn('QWeatherClient.fetchRealtimeWeather: not configured')
            return null
        }

        const locationParam = `${longitude.toFixed(2)},${latitude.toFixed(2)}`

        const locationName = await this.resolveLocationName(locationParam)

        const weatherRoot = await this.requestJson('/v7/weather/now', {
            location: locationParam,
            lang: 'zh',
            unit: 'm',
        })
        if (!weatherRoot) {
            console.error('QWeatherClient.fetchRealtimeWeather: /v7/weather/now returned null')
            return null
        }

        const code = this.getAsString(weatherRoot, 'code')
        if (code !== '200') {
            console.error(`QWeatherClient.fetchRealtimeWeather: API error code=${code}`)
            return null
        }

        const now = weatherRoot['now'] as Record<string, unknown> | undefined
        if (!now) return null

        const liveWeather = this.getAsString(now, 'text')
        const actualWindSpeed = this.parseDouble(this.getAsString(now, 'windSpeed'))
        const windSpeedMps =
            actualWindSpeed > 0
                ? this.round(actualWindSpeed / 3.6)
                : this.parseWindSpeed(this.getAsString(now, 'windScale'))
        const rawVisibility = this.parseDouble(this.getAsString(now, 'vis'))
        const finalVisibility =
            rawVisibility > 0 ? this.round(rawVisibility) : this.deriveVisibility(liveWeather)
        const temperature = this.parseDouble(this.getAsString(now, 'temp'))
        const humidity = this.parseInt(this.getAsString(now, 'humidity'))
        const windDir = this.getAsString(now, 'windDir')
        let windScale = this.getAsString(now, 'windScale')
        if (!windScale.endsWith('级') && windScale !== '') {
            windScale = windScale + '级'
        }

        const realtimePrecip = this.parseDouble(this.getAsString(now, 'precip'))

        const minutelyRoot = await this.requestJson('/v7/minutely/5m', {
            location: locationParam,
            lang: 'zh',
        })

        let precipitationIntensity = realtimePrecip
        let precipitationProbability = this.derivePrecipitationProbability(liveWeather)
        let thunderstormRisk = this.deriveThunderstormRisk(liveWeather)

        if (minutelyRoot && this.getAsString(minutelyRoot, 'code') === '200') {
            const minutely = Array.isArray(minutelyRoot['minutely']) ? minutelyRoot['minutely'] : []
            if (minutely.length > 0) {
                let maxPrecip = 0
                let hasRain = false
                let hasSnow = false
                for (const item of minutely) {
                    if (typeof item === 'object' && item !== null) {
                        const obj = item as Record<string, unknown>
                        maxPrecip = Math.max(maxPrecip, this.parseDouble(this.getAsString(obj, 'precip')))
                        const type = this.getAsString(obj, 'type')
                        if (type === 'rain') hasRain = true
                        if (type === 'snow') hasSnow = true
                    }
                }
                if (maxPrecip > 0) {
                    precipitationIntensity = this.round(maxPrecip * 12.0)
                    precipitationProbability = 90
                } else if (hasRain || hasSnow) {
                    precipitationProbability = Math.max(precipitationProbability, 50)
                }
                const summary = this.getAsString(minutelyRoot, 'summary')
                if (this.containsAny(summary, '雷', '强对流')) {
                    thunderstormRisk = '高'
                }
            }
        }

        const metrics = deriveFromServerData(
            windSpeedMps,
            finalVisibility,
            precipitationProbability,
            precipitationIntensity,
            thunderstormRisk,
        )

        return {
            serviceName: '和风天气',
            locationName,
            adcode: locationParam,
            weather: liveWeather,
            weatherIconType: resolveWeatherIconType(liveWeather),
            reportTime: this.getAsString(now, 'obsTime'),
            fetchedAt: this.formatTime(Date.now()),
            refreshInterval: '15 分钟自动刷新',
            temperature,
            humidity,
            windDirection: windDir,
            windPower: windScale,
            windSpeed: metrics.windSpeed,
            visibility: metrics.visibility,
            precipitationProbability: metrics.precipitationProbability,
            precipitationIntensity: metrics.precipitationIntensity,
            thunderstormRisk: metrics.thunderstormRisk,
            thunderstormRiskLevel: metrics.thunderstormRiskLevel,
            thunderstormRiskLabel: metrics.thunderstormRiskLabel,
            thunderstormRiskHint: metrics.thunderstormRiskHint,
            thunderstormProtectionAdvice: metrics.thunderstormProtectionAdvice,
            sourceNote:
                '天气、温度、湿度、风向、风速与能见度来自和风天气实时天气；降水概率、降水强度与雷暴风险依据和风天气实时数据综合推导。',
            suitability: buildSuitabilityView(metrics),
        }
    }

    private async resolveLocationName(locationParam: string): Promise<string> {
        try {
            const root = await this.requestJson('/geo/v2/city/lookup', {
                location: locationParam,
                number: '1',
                lang: 'zh',
            })
            if (!root || this.getAsString(root, 'code') !== '200') {
                return `当前位置(${locationParam})`
            }
            const locations = Array.isArray(root['location']) ? root['location'] : []
            if (locations.length === 0) {
                return `当前位置(${locationParam})`
            }
            const item = locations[0] as Record<string, unknown>
            const parts: string[] = []
            const adm1 = this.getAsString(item, 'adm1')
            const adm2 = this.getAsString(item, 'adm2')
            const name = this.getAsString(item, 'name')
            if (adm1) parts.push(adm1)
            if (adm2 && adm2 !== adm1) parts.push(adm2)
            if (name) parts.push(name)
            const result = parts.join(' ').trim()
            return result || `当前位置(${locationParam})`
        } catch {
            return `当前位置(${locationParam})`
        }
    }

    private async requestJson(
        path: string,
        params: Record<string, string>,
    ): Promise<Record<string, unknown> | null> {
        try {
            const url = new URL(path, this.apiHost)
            for (const [key, value] of Object.entries(params)) {
                url.searchParams.set(key, value)
            }

            const controller = new AbortController()
            const timeoutId = setTimeout(() => controller.abort(), CALL_TIMEOUT_MS)

            try {
                const response = await fetch(url.toString(), {
                    method: 'GET',
                    headers: {
                        'X-QW-Api-Key': this.apiKey,
                        Accept: 'application/json',
                    },
                    signal: controller.signal,
                })

                if (!response.ok) {
                    console.error(
                        `QWeatherClient.requestJson: HTTP ${response.status} for ${path}`,
                    )
                    return null
                }

                const body = await response.text()
                if (!body) {
                    console.error(`QWeatherClient.requestJson: empty body for ${path}`)
                    return null
                }

                return JSON.parse(body) as Record<string, unknown>
            } finally {
                clearTimeout(timeoutId)
            }
        } catch (error: unknown) {
            if (error instanceof DOMException && error.name === 'AbortError') {
                console.error(`QWeatherClient.requestJson: Timeout for ${path}`)
            } else if (error instanceof TypeError) {
                console.error(`QWeatherClient.requestJson: Network error for ${path}`)
            } else {
                console.error(`QWeatherClient.requestJson: ${String(error)} for ${path}`)
            }
            return null
        }
    }

    private parseDouble(value: string): number {
        try {
            const parsed = parseFloat(value)
            return Number.isFinite(parsed) ? parsed : 0.0
        } catch {
            return 0.0
        }
    }

    private parseInt(value: string): number {
        try {
            const parsed = Number.parseInt(value, 10)
            return Number.isFinite(parsed) ? parsed : 0
        } catch {
            return 0
        }
    }

    private round(value: number): number {
        return Math.round(value * 10) / 10
    }

    private parseWindSpeed(windPowerText: string): number {
        if (!windPowerText?.trim()) return 0.0
        const normalized = windPowerText.replace('≤', '').replace('级', '').trim()
        const segments = normalized.split('-')
        try {
            const level = Number.parseInt(segments[segments.length - 1].trim(), 10)
            return WIND_SPEED_LEVELS[level] ?? 0.0
        } catch {
            return 0.0
        }
    }

    private getAsString(obj: Record<string, unknown>, key: string): string {
        if (!obj) return ''
        const element = obj[key]
        if (element == null) return ''
        return String(element)
    }

    private formatTime(timestamp: number): string {
        const date = new Date(timestamp)
        const pad = (n: number) => String(n).padStart(2, '0')
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }

    private deriveVisibility(weather: string): number {
        if (this.containsAny(weather, '暴雨', '大暴雨')) return 1.0
        if (this.containsAny(weather, '大雨')) return 2.0
        if (this.containsAny(weather, '中雨')) return 4.0
        if (this.containsAny(weather, '小雨', '阵雨')) return 6.0
        if (this.containsAny(weather, '雾', '霾')) return 3.0
        if (this.containsAny(weather, '阴')) return 10.0
        if (this.containsAny(weather, '多云')) return 14.0
        if (this.containsAny(weather, '晴')) return 18.0
        return 12.0
    }

    private derivePrecipitationProbability(weather: string): number {
        if (this.containsAny(weather, '暴雨', '大暴雨')) return 98
        if (this.containsAny(weather, '大雨')) return 92
        if (this.containsAny(weather, '中雨')) return 82
        if (this.containsAny(weather, '小雨', '阵雨')) return 62
        if (this.containsAny(weather, '雷阵雨')) return 90
        if (this.containsAny(weather, '阴')) return 25
        if (this.containsAny(weather, '多云')) return 12
        if (this.containsAny(weather, '晴')) return 5
        return 15
    }

    private deriveThunderstormRisk(weather: string): string {
        if (this.containsAny(weather, '雷', '强对流')) return '高'
        return '低'
    }

    private containsAny(text: string | undefined | null, ...keywords: string[]): boolean {
        if (!text) return false
        for (const keyword of keywords) {
            if (text.includes(keyword)) return true
        }
        return false
    }
}

export const qWeatherClient = new QWeatherClient()
