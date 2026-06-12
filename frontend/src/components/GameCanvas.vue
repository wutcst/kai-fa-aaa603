<template>
  <div ref="gameContainer" style="width:800px;height:600px;border:1px solid #ccc;background:#000"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import Phaser from 'phaser'

const emit = defineEmits(['update'])
const gameContainer = ref(null)
let game = null

// Helper to map arrow keys to directions used by backend
const keyToDir = (key) => {
  switch (key) {
    case 'ArrowUp':
    case 'w':
    case 'W':
      return 'north'
    case 'ArrowDown':
    case 's':
    case 'S':
      return 'south'
    case 'ArrowLeft':
    case 'a':
    case 'A':
      return 'west'
    case 'ArrowRight':
    case 'd':
    case 'D':
      return 'east'
    default:
      return null
  }
}

onMounted(() => {
  const config = {
    type: Phaser.AUTO,
    width: 800,
    height: 600,
    parent: gameContainer.value,
    backgroundColor: '#333333',
    scene: {
      preload: function () {
        // no external assets for demo; we'll use simple shapes and text
      },
      create: async function () {
        const scene = this

        // container groups
        scene.roomGraphics = scene.add.container(0, 0)
        scene.itemsGroup = scene.add.group()
        scene.exitButtons = []

        // draw base background
        const bg = scene.add.graphics()
        bg.fillStyle(0x2d2d2d, 1)
        bg.fillRect(0, 0, 800, 600)
        scene.roomGraphics.add(bg)

        // title and description
        scene.titleText = scene.add.text(20, 20, '', { font: '20px Arial', fill: '#ffffff' })
        scene.descText = scene.add.text(20, 50, '', { font: '14px Arial', fill: '#cccccc', wordWrap: { width: 760 } })

        // player (simple circle)
        scene.player = scene.add.circle(400, 320, 16, 0x00aaff).setStrokeStyle(2, 0x000000)
        scene.playerLabel = scene.add.text(380, 340, 'You', { font: '12px Arial', fill: '#fff' })

        // hint
        scene.add.text(20, 560, '使用方向键 / 点击出口 / 点击物品 与后端交互', { font: '14px Arial', fill: '#cccccc' })

        // function to send command to backend and process result
        scene.sendCommand = async function (cmd) {
          try {
            const res = await fetch('/api/command', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ command: cmd })
            })
            const j = await res.json()
            emit('update', j)
            // if response contains room data, re-render
            if (j && j.data) {
              scene.renderRoom(j.data)
            }
          } catch (e) {
            emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
          }
        }

        // render room view given backend room info
        scene.renderRoom = function (roomInfo) {
          // clear previous item sprites and state
          scene.itemsGroup.clear(true, true)
          scene.itemsData = []
          // remove exit buttons
          scene.exitButtons.forEach(b => b.destroy && b.destroy())
          scene.exitButtons = []

          // update texts
          scene.titleText.setText(roomInfo.name || '未知房间')
          scene.descText.setText(roomInfo.description || '')

          // parse exits string (e.g. "east west") into array
          const exitsStr = roomInfo.exits || ''
          const exits = exitsStr.split(/\s+/).filter(s => s)

          // draw exit (door) visuals at edges but DO NOT auto-send command on click
          const exitPositions = {
            north: { x: 400, y: 80 },
            south: { x: 400, y: 520 },
            west: { x: 120, y: 320 },
            east: { x: 680, y: 320 }
          }

          // clear previous door rects
          scene.doorRects.forEach(d => { try { d.rect.destroy(); d.label.destroy() } catch (e) {} })
          scene.doorRects = []

          exits.forEach(dir => {
            const pos = exitPositions[dir] || { x: 400, y: 80 }
            // choose door size based on orientation
            let w = 120, h = 40
            if (dir === 'west' || dir === 'east') { w = 40; h = 120 }
            const rect = scene.add.rectangle(pos.x, pos.y, w, h, 0x664422).setStrokeStyle(2, 0x222222)
            const label = scene.add.text(pos.x - 20, pos.y - 10, dir.toUpperCase(), { font: '14px Arial', fill: '#ffffff' })
            // clicking a door only moves the player to its area (does NOT immediately enter)
            rect.setInteractive({ useHandCursor: true })
            rect.on('pointerdown', () => {
              scene.tweens.add({ targets: scene.player, x: pos.x, y: pos.y, duration: 300 })
            })
            scene.exitButtons.push(rect)
            scene.exitButtons.push(label)
            scene.doorRects.push({ dir, rect, label })
          })

          // draw items
          const items = roomInfo.items || []
          // layout item positions
          const startX = 520
          let ix = 0
          items.forEach(item => {
            const x = startX + (ix % 2) * 60
            const y = 360 + Math.floor(ix / 2) * 60
            const circle = scene.add.circle(x, y, 20, 0x8b5a2b).setStrokeStyle(2, 0x000000)
            const label = scene.add.text(x - 24, y + 24, item.name, { font: '14px Arial', fill: '#fff' })
            circle.setInteractive({ useHandCursor: true })
            circle.on('pointerover', () => circle.setScale(1.05))
            circle.on('pointerout', () => circle.setScale(1))
            circle.on('pointerdown', async () => {
              // immediate click-based take (still allowed)
              try {
                const res = await fetch('/api/command', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ command: 'take ' + item.name })
                })
                const j = await res.json()
                emit('update', j)
                if (j && j.status === 'success') {
                  scene.tweens.add({ targets: [circle, label], y: '-=100', alpha: 0, scale: 0.5, duration: 500, onComplete: () => { circle.destroy(); label.destroy() } })
                }
                if (j && j.data) scene.renderRoom(j.data)
              } catch (e) {
                emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
              }
            })
            scene.itemsGroup.add(circle)
            scene.itemsGroup.add(label)
            // store in itemsData for proximity-based prompt handling
            scene.itemsData.push({ name: item.name, x, y, circle, label, prompted: false, _removed: false })
            ix++
          })

          // if teleport room, show indicator
          if (roomInfo.isTeleportRoom) {
            scene.add.text(600, 20, '传送房间', { font: '14px Arial', fill: '#ffcc00' })
          }
        }

        // fetch initial game state from backend
        try {
          const res = await fetch('/api/game')
          const j = await res.json()
          emit('update', j)
          if (j && j.data) scene.renderRoom(j.data)
        } catch (e) {
          emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
        }
        // set up keyboard (WASD + E for pickup)
        scene.keys = scene.input.keyboard.addKeys('W,A,S,D,E')
        scene.moveSpeed = 160 // pixels per second
        scene.lastDoorEntered = null
        scene.doorRects = []
        scene.itemsData = []

        // update loop for movement / proximity checks
        this.sys.events.on('update', function (time, delta) {
          const dt = delta / 1000
          // movement by WASD
          let vx = 0, vy = 0
          if (scene.keys.W.isDown) vy -= 1
          if (scene.keys.S.isDown) vy += 1
          if (scene.keys.A.isDown) vx -= 1
          if (scene.keys.D.isDown) vx += 1
          // normalize
          if (vx !== 0 || vy !== 0) {
            const len = Math.sqrt(vx*vx + vy*vy)
            vx = vx / len
            vy = vy / len
            scene.player.x += vx * scene.moveSpeed * dt
            scene.player.y += vy * scene.moveSpeed * dt
          }
          // keep player inside bounds
          scene.player.x = Phaser.Math.Clamp(scene.player.x, 16, 800 - 16)
          scene.player.y = Phaser.Math.Clamp(scene.player.y, 16, 600 - 16)
          scene.playerLabel.setPosition(scene.player.x - 20, scene.player.y + 18)

          // door overlap detection: only when player center is inside door rect bounds
          let insideAnyDoor = false
          for (const dr of scene.doorRects) {
            const b = dr.rect.getBounds()
            if (scene.player.x >= b.left && scene.player.x <= b.right && scene.player.y >= b.top && scene.player.y <= b.bottom) {
              insideAnyDoor = true
              if (scene.lastDoorEntered !== dr.dir) {
                scene.lastDoorEntered = dr.dir
                // send go command once upon entering the door area
                scene.sendCommand('go ' + dr.dir)
              }
              break
            }
          }
          if (!insideAnyDoor) scene.lastDoorEntered = null

          // item proximity checks (pickup prompt)
          const pickupRadius = 40
          for (let i = scene.itemsData.length - 1; i >= 0; i--) {
            const it = scene.itemsData[i]
            if (!it || it._removed) continue
            const dist = Phaser.Math.Distance.Between(scene.player.x, scene.player.y, it.x, it.y)
            if (dist <= pickupRadius) {
              if (!it.prompted) {
                it.prompted = true
                // ask user whether to pick up
                try {
                  const ok = window.confirm('是否将 ' + it.name + ' 放入背包？')
                  if (ok) {
                    ;(async () => {
                      try {
                        const res = await fetch('/api/command', {
                          method: 'POST',
                          headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify({ command: 'take ' + it.name })
                        })
                        const j = await res.json()
                        emit('update', j)
                        if (j && j.status === 'success') {
                          // remove visuals
                          it.circle.destroy()
                          it.label.destroy()
                          it._removed = true
                          scene.itemsData.splice(i, 1)
                        }
                        if (j && j.data) scene.renderRoom(j.data)
                      } catch (e) {
                        emit('update', { status: 'error', message: '无法连接后端: ' + e.message, data: null })
                      }
                    })()
                  }
                } catch (e) { /* ignore */ }
              }
            } else {
              // reset prompt flag when leaving range so user can be asked again later
              if (it.prompted) it.prompted = false
            }
          }
        })

        // listen to parent/other UI updates (e.g., reset) and re-render room
        window.__zuul_game_update_handler = (ev) => {
          try {
            if (ev && ev.detail && ev.detail.data) {
              scene.renderRoom(ev.detail.data)
            }
          } catch (e) { /* ignore */ }
        }
        window.addEventListener('game:update', window.__zuul_game_update_handler)
      },
      destroy: function () {
        // placeholder
      }
    }
  }

  game = new Phaser.Game(config)
})

onBeforeUnmount(() => {
  if (game) {
    try { game.destroy(true) } catch (e) {}
    game = null
  }
  // remove global handlers if present
  try {
    if (window.__zuul_key_handler) {
      window.removeEventListener('keydown', window.__zuul_key_handler)
      delete window.__zuul_key_handler
    }
    if (window.__zuul_game_update_handler) {
      window.removeEventListener('game:update', window.__zuul_game_update_handler)
      delete window.__zuul_game_update_handler
    }
  } catch (e) { /* ignore */ }
})
</script>

