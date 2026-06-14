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
        // store background color for later VFX masking
        scene.bgColor = 0x2d2d2d
        bg.fillStyle(scene.bgColor, 1)
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
        // set up keyboard (WASD + Shift + E + J for attack)
        // Note: holding Shift alone should NOT move the player; holding Shift + movement keys doubles speed
        scene.keys = scene.input.keyboard.addKeys('W,A,S,D,E,SHIFT,J')
        // base movement speed (pixels per second)
        scene.baseMoveSpeed = 160
        // facing angle in radians; used as attack central axis. Default to facing right (0 rad).
        scene.facingAngle = 0
        // attack / VFX configuration (tweak these to change visual tempo)
        scene.attackConfig = {
          radius: 110,
          angleDeg: 135,
          // increase polygon segments for smoother shape
          segments: 96,
          // sweep animation duration (ms)
          sweepDuration: 140,
          // base ghost (afterimage) fade duration (ms) - smaller value for faster disappearance
          ghostFade: 120,
          // final main fade duration after sweep completes (ms)
          finalFade: 60,
          // how often (fraction of progress) to spawn ghosts — smaller = more ghosts (higher density)
          ghostSpacing: 0.035,
          // main alpha values
          mainAlpha: 0.95,
          // increase ghost alpha to make blocks more visible
          ghostAlpha: 0.92,
          // minimum per-ghost fade (ms)
          ghostMinFade: 40,
          // pierce (forward dash) config
          pierceDistance: 120,
          // expand pierce effective range by this multiplier
          pierceDistanceExpand: 1.15,
          pierceDuration: 100,
          pierceFade: 180,
          pierceWidth: 14
        }
        // counter to assign increasing depth to ghosts so later ghosts appear above earlier ones
        scene._ghostCounter = 0
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
            // update facing direction to last movement direction
            try { scene.facingAngle = Math.atan2(vy, vx) } catch (e) { /* ignore */ }
            // determine speed: double when Shift is held together with a movement key
            let speed = scene.baseMoveSpeed
            try {
              if (scene.keys.SHIFT && scene.keys.SHIFT.isDown) {
                speed = speed * 2
              }
            } catch (e) { /* ignore if key not present */ }
            scene.player.x += vx * speed * dt
            scene.player.y += vy * speed * dt
          }

          // handle attack key (J) pressed -> draw a sweeping blade that traces the sector clockwise, then fade out with ghosts
          try {
            if (scene.keys.J && Phaser.Input.Keyboard.JustDown(scene.keys.J)) {
              const cfg = scene.attackConfig || {}
              // if Shift + movement key(s) are held, perform a forward pierce/dash attack instead of sweep
              const isShiftMove = (scene.keys.SHIFT && scene.keys.SHIFT.isDown) && (scene.keys.W.isDown || scene.keys.A.isDown || scene.keys.S.isDown || scene.keys.D.isDown)
              if (isShiftMove) {
                // forward pierce: tween player forward a short distance and draw a thin elongated effect that fades
                const startX = scene.player.x
                const startY = scene.player.y
                const dx = Math.cos(scene.facingAngle)
                const dy = Math.sin(scene.facingAngle)
                const dist = cfg.pierceDistance || 120
                const targetX = Phaser.Math.Clamp(startX + dx * dist, 16, 800 - 16)
                const targetY = Phaser.Math.Clamp(startY + dy * dist, 16, 600 - 16)
                // tween player movement
                scene.tweens.add({ targets: scene.player, x: targetX, y: targetY, duration: cfg.pierceDuration || 100, ease: 'Cubic.easeOut' })
                // draw pierce effect: diamond (rhombus) spanning from start to target, then fade
                try {
                  const g2 = scene.add.graphics()
                  const extra = cfg.pierceDistanceExpand || 1.0
                  const effTargetX = Phaser.Math.Clamp(startX + dx * dist * extra, 16, 800 - 16)
                  const effTargetY = Phaser.Math.Clamp(startY + dy * dist * extra, 16, 600 - 16)
                  // midpoint
                  const mx = (startX + effTargetX) / 2
                  const my = (startY + effTargetY) / 2
                  const w = cfg.pierceWidth || 12
                  // perpendicular unit
                  const px = -dy
                  const py = dx
                  const hx = (px * (w/2))
                  const hy = (py * (w/2))
                  const front = { x: effTargetX, y: effTargetY }
                  const right = { x: mx + hx, y: my + hy }
                  const back = { x: startX, y: startY }
                  const left = { x: mx - hx, y: my - hy }
                  g2.fillStyle(0xC0C0C0, cfg.mainAlpha || 0.95)
                  g2.fillPoints([front, right, back, left], true)
                  // fade and destroy
                  scene.tweens.add({ targets: g2, alpha: 0, duration: cfg.pierceFade || 180, onComplete: () => { try { g2.destroy() } catch (e) {} } })
                } catch (e) { /* ignore drawing issues */ }
                // done with pierce path
              } else {
              const cx = scene.player.x
              const cy = scene.player.y
              const radius = cfg.radius || 110
              const spanRad = Phaser.Math.DegToRad(cfg.angleDeg || 135)
              const half = spanRad / 2
              const startAngle = scene.facingAngle - half
              const segments = cfg.segments || 30

              const redraw = (gfx, progress, alpha = (cfg.mainAlpha || 0.85), color = 0xff4444) => {
                gfx.clear()
                // slightly increase radius during sweep for a dynamic feel
                const grow = cfg.radiusGrow || 0.08
                const adjRadius = radius * (1 + grow * Phaser.Math.Clamp(progress, 0, 1))
                gfx.fillStyle(color, alpha)
                const usedSpan = spanRad * Phaser.Math.Clamp(progress, 0, 1)
                const endAngle = startAngle + usedSpan
                const points = []
                points.push({ x: cx, y: cy })
                for (let i = 0; i <= segments; i++) {
                  const t = i / segments
                  const ang = startAngle + (endAngle - startAngle) * t
                  const px = cx + Math.cos(ang) * adjRadius
                  const py = cy + Math.sin(ang) * adjRadius
                  points.push({ x: px, y: py })
                }
                gfx.fillPoints(points, true)
                // draw a pale silver ring near center (outer silver, inner masked by background color)
                try {
                  const ringInner = Math.max(4, Math.round(adjRadius * (cfg.ringInnerFactor || 0.12)))
                  const ringThickness = Math.max(3, Math.round(adjRadius * (cfg.ringThicknessFactor || 0.06)))
                  const ringOuter = ringInner + ringThickness
                  // outer pale silver
                  gfx.fillStyle(0xC0C0C0, alpha * (cfg.ringAlphaFactor || 0.9))
                  gfx.fillCircle(cx, cy, ringOuter)
                  // mask inner with background color to form a ring hole
                  gfx.fillStyle(scene.bgColor || 0x2d2d2d, 1)
                  gfx.fillCircle(cx, cy, ringInner)
                } catch (e) { /* ignore */ }
              }

              const g = scene.add.graphics()
              redraw(g, 0)

              let lastGhostT = -1
              const ghostSpacing = cfg.ghostSpacing || 0.12
              const ghostFade = cfg.ghostFade || 150
              const sweepDuration = cfg.sweepDuration || 150
              const finalFade = cfg.finalFade || 100

              const prog = { t: 0 }
              scene.tweens.add({
                targets: prog,
                t: 1,
                duration: sweepDuration,
                ease: 'Linear',
                onUpdate: () => {
                  try {
                    redraw(g, prog.t)
                                      if (prog.t - lastGhostT >= ghostSpacing) {
                        lastGhostT = prog.t
                        const ghost = scene.add.graphics()
                        // draw ghost with configured alpha and color
                                        const gAlpha = cfg.ghostAlpha || 0.92
                                        // use pale silver for ghost color
                                        redraw(ghost, prog.t, gAlpha, 0xC0C0C0)
                        // ensure later ghosts are above earlier ones
                        try { ghost.setDepth(scene._ghostCounter++) } catch (e) {}
                        // earlier ghosts should fade sooner than later ghosts: compute duration based on progress (earlier prog.t -> shorter duration)
                        const minFade = cfg.ghostMinFade || 40
                                        const fadeDur = Math.max(minFade, Math.round(ghostFade * (0.15 + prog.t * 0.85)))
                                        // make fades overall faster by scaling down
                                        const scaledFade = Math.max(minFade, Math.round(fadeDur * 0.7))
                                        scene.tweens.add({ targets: ghost, alpha: 0, duration: scaledFade, onComplete: () => { try { ghost.destroy() } catch (e) {} } })
                      }
                  } catch (e) { /* ignore drawing issues */ }
                },
                onComplete: () => {
                  scene.tweens.add({ targets: g, alpha: 0, duration: finalFade, onComplete: () => { try { g.destroy() } catch (e) {} } })
                }
               })
              }
             }
          } catch (e) { /* ignore input issues */ }

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

