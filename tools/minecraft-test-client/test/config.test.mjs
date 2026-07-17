import test from 'node:test'
import assert from 'node:assert/strict'

import { readClientSettings } from '../lib/config.mjs'

test('uses a loopback offline client by default', () => {
    assert.deepEqual(readClientSettings({}), {
        allowRemote: false,
        auth: 'offline',
        host: '127.0.0.1',
        port: 25565,
        username: 'BigCasaresTestBot',
        version: false
    })
})

test('rejects a remote target without an explicit opt-in', () => {
    assert.throws(
        () => readClientSettings({ MC_HOST: 'play.example.com' }),
        /MC_ALLOW_REMOTE=true/
    )
})

test('allows an explicitly opted-in remote target', () => {
    const settings = readClientSettings({
        MC_ALLOW_REMOTE: 'true',
        MC_AUTH: 'microsoft',
        MC_HOST: 'play.example.com',
        MC_PORT: '25570',
        MC_USERNAME: 'player@example.com',
        MC_VERSION: '26.2'
    })

    assert.deepEqual(settings, {
        allowRemote: true,
        auth: 'microsoft',
        host: 'play.example.com',
        port: 25570,
        username: 'player@example.com',
        version: '26.2'
    })
})

test('rejects malformed ports and authentication modes', () => {
    assert.throws(() => readClientSettings({ MC_PORT: '0' }), /MC_PORT/)
    assert.throws(() => readClientSettings({ MC_PORT: 'invalid' }), /MC_PORT/)
    assert.throws(() => readClientSettings({ MC_AUTH: 'token' }), /MC_AUTH/)
})
