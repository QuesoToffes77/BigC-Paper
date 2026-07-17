import { createInterface } from 'node:readline'

import mineflayer from 'mineflayer'

import { readClientSettings } from './lib/config.mjs'
import { MinecraftPuppet } from './lib/puppet.mjs'

const settings = readClientSettings(process.env)
const puppet = new MinecraftPuppet(mineflayer, settings, emit)

process.on('SIGINT', () => shutdown(0))
process.on('SIGTERM', () => shutdown(0))

try {
    await puppet.connect()
    const input = createInterface({ input: process.stdin, crlfDelay: Infinity })
    for await (const line of input) {
        if (line.trim().length === 0) {
            continue
        }
        await dispatch(line)
    }
    await shutdown(0)
} catch (error) {
    emit({ message: error.message, type: 'fatal' })
    await shutdown(1)
}

async function dispatch(line) {
    let request
    try {
        request = JSON.parse(line)
    } catch {
        emit({ message: 'each input line must be valid JSON', type: 'actionError' })
        return
    }
    try {
        await puppet.act(request)
    } catch (error) {
        emit({ action: request?.action ?? null, message: error.message, type: 'actionError' })
    }
}

async function shutdown(exitCode) {
    await puppet.close()
    process.exitCode = exitCode
}

function emit(event) {
    process.stdout.write(`${JSON.stringify(event)}\n`)
}
