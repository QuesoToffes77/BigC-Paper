const CONTROL_STATES = new Set(['forward', 'back', 'left', 'right', 'jump', 'sprint', 'sneak'])

export class MinecraftPuppet {
    constructor(mineflayer, settings, emit) {
        this.mineflayer = mineflayer
        this.settings = settings
        this.emit = emit
        this.bot = null
    }

    async connect() {
        if (this.bot !== null) {
            throw new Error('client is already connected')
        }
        const bot = this.mineflayer.createBot({
            auth: this.settings.auth,
            host: this.settings.host,
            port: this.settings.port,
            username: this.settings.username,
            version: this.settings.version
        })
        this.bot = bot
        this.observe(bot)
        try {
            await waitForEvent(bot, 'spawn')
        } catch (error) {
            if (this.bot === bot) {
                this.bot = null
            }
            throw error
        }
        this.emit({ type: 'ready', username: bot.username })
    }

    async act(request) {
        const bot = this.requireBot()
        const action = request?.action
        if (typeof action !== 'string') {
            throw new Error('action must be a string')
        }

        switch (action) {
            case 'chat':
                bot.chat(readText(request.message, 'message'))
                return
            case 'command':
                bot.chat(`/${readText(request.command, 'command').replace(/^\/+/, '')}`)
                return
            case 'setControlState':
                this.setControlState(bot, request)
                return
            case 'look':
                await bot.look(readNumber(request.yaw, 'yaw'), readNumber(request.pitch, 'pitch'), true)
                return
            case 'swingArm':
                bot.swingArm()
                return
            case 'inventory':
                this.emit({ items: bot.inventory.items().map(summarizeItem), type: 'inventory' })
                return
            case 'position':
                this.emit({ position: summarizePosition(bot.entity?.position), type: 'position' })
                return
            case 'waitForChat':
                await this.waitForChat(bot, request)
                return
            case 'quit':
                bot.quit('BigCasares test client shutdown')
                return
            default:
                throw new Error(`unsupported action: ${action}`)
        }
    }

    async close() {
        if (this.bot !== null) {
            this.bot.quit('BigCasares test client shutdown')
            this.bot = null
        }
    }

    observe(bot) {
        bot.once('login', () => this.emit({ type: 'login', username: bot.username }))
        bot.on('message', (message, position, sender, verified) => {
            this.emit({
                message: message.toString(),
                position,
                sender: sender ?? null,
                type: 'chat',
                verified: verified ?? null
            })
        })
        bot.on('kicked', (reason, loggedIn) => {
            this.emit({ loggedIn, reason: stringifyReason(reason), type: 'kicked' })
        })
        bot.on('error', error => this.emit({ message: error.message, type: 'error' }))
        bot.on('end', reason => {
            this.bot = null
            this.emit({ reason: stringifyReason(reason), type: 'end' })
        })
    }

    setControlState(bot, request) {
        const control = readText(request.control, 'control')
        if (!CONTROL_STATES.has(control)) {
            throw new Error(`unsupported control: ${control}`)
        }
        if (typeof request.value !== 'boolean') {
            throw new Error('value must be a boolean')
        }
        bot.setControlState(control, request.value)
    }

    async waitForChat(bot, request) {
        const pattern = readText(request.pattern, 'pattern')
        const timeoutMs = request.timeoutMs === undefined ? 5000 : readTimeout(request.timeoutMs)
        const message = await new Promise((resolve, reject) => {
            const timeout = setTimeout(() => {
                bot.removeListener('message', onMessage)
                reject(new Error(`timed out waiting for chat containing: ${pattern}`))
            }, timeoutMs)
            const onMessage = chatMessage => {
                const text = chatMessage.toString()
                if (!text.includes(pattern)) {
                    return
                }
                clearTimeout(timeout)
                bot.removeListener('message', onMessage)
                resolve(text)
            }
            bot.on('message', onMessage)
        })
        this.emit({ message, pattern, type: 'chatMatch' })
    }

    requireBot() {
        if (this.bot === null) {
            throw new Error('client is not connected')
        }
        return this.bot
    }
}

function waitForEvent(emitter, event) {
    return new Promise((resolve, reject) => {
        const onError = error => {
            emitter.removeListener(event, onEvent)
            reject(error)
        }
        const onEvent = () => {
            emitter.removeListener('error', onError)
            resolve()
        }
        emitter.once('error', onError)
        emitter.once(event, onEvent)
    })
}

function readText(value, name) {
    if (typeof value !== 'string' || value.trim().length === 0) {
        throw new Error(`${name} must be a non-empty string`)
    }
    return value.trim()
}

function readNumber(value, name) {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
        throw new Error(`${name} must be a finite number`)
    }
    return value
}

function readTimeout(value) {
    if (!Number.isInteger(value) || value < 1 || value > 60000) {
        throw new Error('timeoutMs must be an integer between 1 and 60000')
    }
    return value
}

function summarizeItem(item) {
    return {
        count: item.count,
        metadata: item.metadata,
        name: item.name,
        nbt: item.nbt ? JSON.stringify(item.nbt) : null,
        slot: item.slot,
        type: item.type
    }
}

function summarizePosition(position) {
    if (position === undefined) {
        return null
    }
    return { x: position.x, y: position.y, z: position.z }
}

function stringifyReason(reason) {
    return typeof reason === 'string' ? reason : JSON.stringify(reason)
}
