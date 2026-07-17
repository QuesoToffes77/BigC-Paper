const DEFAULT_HOST = '127.0.0.1'
const DEFAULT_PORT = 25565
const DEFAULT_USERNAME = 'BigCasaresTestBot'
const SUPPORTED_AUTH = new Set(['offline', 'microsoft'])

export function readClientSettings(environment) {
    const host = readText(environment.MC_HOST, DEFAULT_HOST).toLowerCase()
    const allowRemote = readBoolean(environment.MC_ALLOW_REMOTE, false, 'MC_ALLOW_REMOTE')
    const auth = readText(environment.MC_AUTH, 'offline').toLowerCase()
    const username = readText(environment.MC_USERNAME, DEFAULT_USERNAME)
    const port = readPort(environment.MC_PORT)
    const version = readOptionalText(environment.MC_VERSION) ?? false

    if (!SUPPORTED_AUTH.has(auth)) {
        throw new Error('MC_AUTH must be offline or microsoft')
    }
    if (!isLoopbackHost(host) && !allowRemote) {
        throw new Error('MC_HOST is not loopback; set MC_ALLOW_REMOTE=true to opt in')
    }
    if (username.length === 0) {
        throw new Error('MC_USERNAME must not be empty')
    }

    return Object.freeze({ allowRemote, auth, host, port, username, version })
}

function readPort(value) {
    if (value === undefined || value === '') {
        return DEFAULT_PORT
    }
    if (!/^\d+$/.test(value)) {
        throw new Error('MC_PORT must be an integer between 1 and 65535')
    }
    const port = Number(value)
    if (!Number.isSafeInteger(port) || port < 1 || port > 65535) {
        throw new Error('MC_PORT must be an integer between 1 and 65535')
    }
    return port
}

function readBoolean(value, fallback, name) {
    if (value === undefined || value === '') {
        return fallback
    }
    if (value === 'true') {
        return true
    }
    if (value === 'false') {
        return false
    }
    throw new Error(`${name} must be true or false`)
}

function readText(value, fallback) {
    const text = readOptionalText(value)
    return text ?? fallback
}

function readOptionalText(value) {
    if (value === undefined) {
        return null
    }
    const text = value.trim()
    return text.length === 0 ? null : text
}

function isLoopbackHost(host) {
    return host === 'localhost' || host === '::1' || host.startsWith('127.')
}
