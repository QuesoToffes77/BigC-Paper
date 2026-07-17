import { EventEmitter } from 'node:events'
import assert from 'node:assert/strict'
import test from 'node:test'

import { MinecraftPuppet } from '../lib/puppet.mjs'

test('connects with local settings and dispatches constrained actions', async () => {
    const bot = new FakeBot()
    const emitted = []
    const mineflayer = {
        createBot(options) {
            bot.options = options
            queueMicrotask(() => bot.emit('spawn'))
            return bot
        }
    }
    const puppet = new MinecraftPuppet(mineflayer, settings(), event => emitted.push(event))

    await puppet.connect()
    await puppet.act({ action: 'command', command: '/bigcasares items info' })
    await puppet.act({ action: 'setControlState', control: 'forward', value: true })
    await puppet.act({ action: 'look', pitch: 0.5, yaw: 1.25 })
    await puppet.act({ action: 'inventory' })

    assert.deepEqual(bot.options, {
        auth: 'offline',
        host: '127.0.0.1',
        port: 25565,
        username: 'BigCasaresTestBot',
        version: false
    })
    assert.deepEqual(bot.chats, ['/bigcasares items info'])
    assert.deepEqual(bot.controls, [['forward', true]])
    assert.deepEqual(bot.looks, [[1.25, 0.5, true]])
    assert.deepEqual(emitted.at(-1), {
        items: [{ count: 1, metadata: 0, name: 'apple', nbt: null, slot: 9, type: 260 }],
        type: 'inventory'
    })
})

test('rejects unsupported actions without sending a packet', async () => {
    const bot = new FakeBot()
    const puppet = new MinecraftPuppet({
        createBot() {
            queueMicrotask(() => bot.emit('spawn'))
            return bot
        }
    }, settings(), () => {})

    await puppet.connect()

    await assert.rejects(
        () => puppet.act({ action: 'setControlState', control: 'attack', value: true }),
        /unsupported control/
    )
    await assert.rejects(() => puppet.act({ action: 'mine' }), /unsupported action/)
    assert.deepEqual(bot.chats, [])
})

test('clears a failed connection before shutdown', async () => {
    const bot = new FakeBot()
    const puppet = new MinecraftPuppet({
        createBot() {
            queueMicrotask(() => bot.emit('error', new Error('connection refused')))
            return bot
        }
    }, settings(), () => {})

    await assert.rejects(() => puppet.connect(), /connection refused/)
    await puppet.close()

    assert.equal(bot.quitCalls, 0)
})

function settings() {
    return {
        allowRemote: false,
        auth: 'offline',
        host: '127.0.0.1',
        port: 25565,
        username: 'BigCasaresTestBot',
        version: false
    }
}

class FakeBot extends EventEmitter {
    constructor() {
        super()
        this.chats = []
        this.controls = []
        this.entity = { position: { x: 1, y: 2, z: 3 } }
        this.inventory = {
            items: () => [{ count: 1, metadata: 0, name: 'apple', slot: 9, type: 260 }]
        }
        this.looks = []
        this.quitCalls = 0
        this.username = 'BigCasaresTestBot'
    }

    chat(message) {
        this.chats.push(message)
    }

    look(yaw, pitch, force) {
        this.looks.push([yaw, pitch, force])
        return Promise.resolve()
    }

    quit() {
        this.quitCalls += 1
        this.emit('end', 'quit')
    }

    setControlState(control, value) {
        this.controls.push([control, value])
    }

    swingArm() {
    }
}
