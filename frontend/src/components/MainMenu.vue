<template>
  <div class="main-menu">
    <!-- 背景层 -->
    <div class="menu-bg"></div>
    <div class="menu-overlay"></div>

    <!-- 飘浮粒子效果 -->
    <div class="particles">
      <div
        v-for="p in particles"
        :key="p.id"
        class="particle"
        :style="{
          left: p.x + '%',
          top: p.y + '%',
          width: p.size + 'px',
          height: p.size + 'px',
          animationDelay: p.delay + 's',
          animationDuration: p.duration + 's',
          opacity: p.opacity,
        }"
      ></div>
    </div>

    <!-- 标题区域 -->
    <div class="menu-header">
      <div class="title-subtitle">失落的古迹</div>
      <h1 class="game-title">ZUUL</h1>
      <div class="title-divider">
        <span class="divider-line"></span>
        <span class="divider-diamond">◆</span>
        <span class="divider-line"></span>
      </div>
      <p class="title-tagline">深入迷宫，探寻被遗忘的秘密</p>
    </div>

    <!-- 菜单按钮 -->
    <div class="menu-buttons">
      <button class="menu-btn start-btn" @click="$emit('start-game')">
        <span class="btn-icon">⚔</span>
        <span class="btn-text">开始游戏</span>
        <span class="btn-hint">新的冒险</span>
      </button>

      <button class="menu-btn load-btn" @click="$emit('load-game')">
        <span class="btn-icon">📜</span>
        <span class="btn-text">继续冒险</span>
        <span class="btn-hint">读取存档</span>
      </button>

      <button class="menu-btn about-btn" @click="showAbout = true">
        <span class="btn-icon">✦</span>
        <span class="btn-text">关于游戏</span>
        <span class="btn-hint">故事背景</span>
      </button>
    </div>

    <!-- 底部装饰 -->
    <div class="menu-footer">
      <div class="footer-decoration">
        <span class="footer-symbol">▲</span>
        <span class="footer-text">WHUT · 软件工程实践</span>
        <span class="footer-symbol">▲</span>
      </div>
    </div>

    <!-- 关于弹窗 -->
    <Transition name="fade">
      <div v-if="showAbout" class="about-overlay" @click.self="showAbout = false">
        <div class="about-panel">
          <button class="about-close" @click="showAbout = false">✕</button>
          <h2 class="about-title">关于 ZUUL</h2>
          <div class="about-divider"></div>
          <div class="about-content">
            <p>在远古遗迹的深处，沉睡着一个被遗忘的文明。</p>
            <p>你，一位勇敢的探险者，将穿越变幻莫测的迷宫，<br>与神秘的怪物战斗，收集强大的魔法物品，<br>揭示隐藏在废墟中的真相。</p>
            <p class="about-motto">"勇气是人类最伟大的赞歌。"</p>
          </div>
          <div class="about-credits">
            <span>技术栈：Vue 3 + Phaser 3 + Spring Boot</span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

defineEmits(['start-game', 'load-game'])

const showAbout = ref(false)
const particles = ref([])

// 粒子配置常量
const PARTICLE_COUNT = 60
const PARTICLE_CONFIG = {
  sizeMin: 2, sizeMax: 8,
  delayMax: 8,
  durMin: 6, durMax: 14,
  opacityMin: 0.15, opacityMax: 0.55,
}

// 生成飘浮粒子
function generateParticles() {
  const { sizeMin, sizeMax, delayMax, durMin, durMax, opacityMin, opacityMax } = PARTICLE_CONFIG
  particles.value = Array.from({ length: PARTICLE_COUNT }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    size: sizeMin + Math.random() * (sizeMax - sizeMin),
    delay: Math.random() * delayMax,
    duration: durMin + Math.random() * (durMax - durMin),
    opacity: opacityMin + Math.random() * (opacityMax - opacityMin),
  }))
}

onMounted(() => {
  generateParticles()
})
</script>

<style scoped>
/* ============ 容器 ============ */
.main-menu {
  position: relative;
  width: 800px;
  height: 600px;
  overflow: hidden;
  font-family: 'Georgia', 'Times New Roman', serif;
  user-select: none;
}

/* ============ 背景 ============ */
.menu-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 30%, rgba(60, 30, 10, 0.25) 0%, transparent 60%),
    url('../assets/bg0.png') center / cover no-repeat;
  filter: brightness(0.7) saturate(0.8);
}

.menu-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg,
      rgba(0, 0, 0, 0.45) 0%,
      rgba(0, 0, 0, 0.25) 30%,
      rgba(0, 0, 0, 0.50) 70%,
      rgba(0, 0, 0, 0.80) 100%
    ),
    radial-gradient(ellipse at 50% 120%, rgba(80, 40, 15, 0.30) 0%, transparent 70%);
  pointer-events: none;
}

/* ============ 粒子 ============ */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 200, 120, 0.8), rgba(255, 150, 50, 0.2));
  box-shadow: 0 0 6px rgba(255, 180, 80, 0.3);
  animation: float-up linear infinite;
}

@keyframes float-up {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 0.6;
  }
  100% {
    transform: translateY(-120px) scale(0.5);
    opacity: 0;
  }
}

/* ============ 标题 ============ */
.menu-header {
  position: relative;
  z-index: 2;
  text-align: center;
  padding-top: 100px;
}

.title-subtitle {
  font-size: 14px;
  letter-spacing: 6px;
  text-transform: uppercase;
  color: rgba(200, 160, 100, 0.7);
  font-family: 'Arial', sans-serif;
  margin-bottom: 4px;
}

.game-title {
  font-size: 92px;
  font-weight: bold;
  color: #f5d896;
  margin: 0;
  line-height: 1;
  letter-spacing: 14px;
  font-family: 'Georgia', 'Times New Roman', serif;
  /* 多层文字阴影 — 打造浮雕石刻质感 */
  text-shadow:
    /* 外层光晕 */
    0 0 30px rgba(200, 150, 50, 0.5),
    0 0 60px rgba(200, 150, 50, 0.25),
    /* 亮面高光 */
    0 1px 0 rgba(255, 240, 200, 0.7),
    0 2px 0 rgba(220, 180, 80, 0.5),
    /* 暗部凹陷 */
    0 -1px 0 rgba(60, 30, 5, 0.6),
    0 -2px 0 rgba(40, 20, 0, 0.5),
    1px 3px 4px rgba(0, 0, 0, 0.5),
    2px 4px 8px rgba(0, 0, 0, 0.35),
    3px 6px 12px rgba(0, 0, 0, 0.2);
  /* 渐变叠加 */
  background: linear-gradient(
    180deg,
    #fff8e0 0%,
    #f5d896 15%,
    #e8c860 40%,
    #c89b4a 55%,
    #a07030 75%,
    #6a4020 100%
  );
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: title-glow 3s ease-in-out infinite alternate;
}

@keyframes title-glow {
  0% {
    filter: drop-shadow(0 4px 18px rgba(200, 150, 50, 0.35));
  }
  100% {
    filter: drop-shadow(0 6px 30px rgba(220, 170, 60, 0.55));
  }
}

.title-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 12px 0 14px;
}

.divider-line {
  display: block;
  width: 120px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(200, 160, 80, 0.6), transparent);
}

.divider-diamond {
  color: #c89b4a;
  font-size: 12px;
  opacity: 0.7;
}

.title-tagline {
  font-size: 15px;
  color: rgba(200, 180, 140, 0.7);
  letter-spacing: 3px;
  font-style: italic;
  margin: 0;
}

/* ============ 按钮 ============ */
.menu-buttons {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 36px;
}

.menu-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  width: 340px;
  padding: 14px 28px;
  border: 1px solid rgba(200, 160, 80, 0.25);
  border-radius: 4px;
  background: linear-gradient(135deg,
    rgba(30, 20, 10, 0.85) 0%,
    rgba(50, 30, 15, 0.85) 50%,
    rgba(30, 20, 10, 0.85) 100%
  );
  color: #d4c4a0;
  font-family: 'Georgia', serif;
  cursor: pointer;
  transition: all 0.25s ease;
  overflow: hidden;
}

.menu-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg,
    transparent 0%,
    rgba(200, 160, 80, 0.08) 50%,
    transparent 100%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}

.menu-btn:hover {
  border-color: rgba(200, 160, 80, 0.55);
  background: linear-gradient(135deg,
    rgba(40, 28, 14, 0.95) 0%,
    rgba(70, 45, 20, 0.95) 50%,
    rgba(40, 28, 14, 0.95) 100%
  );
  transform: translateX(6px);
  box-shadow:
    0 0 20px rgba(200, 160, 80, 0.12),
    inset 0 0 30px rgba(200, 160, 80, 0.05);
}

.menu-btn:hover::before {
  opacity: 1;
}

.menu-btn:active {
  transform: translateX(3px) scale(0.99);
}

.btn-icon {
  font-size: 22px;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
  filter: drop-shadow(0 0 6px rgba(200, 150, 50, 0.3));
}

.btn-text {
  font-size: 18px;
  font-weight: bold;
  color: #e8d8b0;
  letter-spacing: 2px;
  flex-shrink: 0;
}

.btn-hint {
  margin-left: auto;
  font-size: 12px;
  color: rgba(180, 150, 100, 0.5);
  font-style: italic;
  letter-spacing: 1px;
}

.start-btn .btn-icon { color: #ff8844; }
.load-btn .btn-icon  { color: #88bbff; }
.about-btn .btn-icon { color: #ddbb77; }

/* ============ 底部 ============ */
.menu-footer {
  position: absolute;
  bottom: 18px;
  left: 0;
  right: 0;
  z-index: 2;
  text-align: center;
}

.footer-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.footer-symbol {
  font-size: 10px;
  color: rgba(200, 160, 80, 0.3);
}

.footer-text {
  font-size: 11px;
  color: rgba(200, 180, 140, 0.25);
  letter-spacing: 3px;
  font-family: Arial, sans-serif;
}

/* ============ 关于弹窗 ============ */
.about-overlay {
  position: absolute;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(3px);
}

.about-panel {
  position: relative;
  width: 420px;
  max-height: 80%;
  padding: 36px 32px 28px;
  background: linear-gradient(135deg, #1a1208 0%, #2a1c0e 100%);
  border: 1px solid rgba(200, 160, 80, 0.25);
  border-radius: 6px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.6);
}

.about-close {
  position: absolute;
  top: 10px;
  right: 14px;
  background: none;
  border: none;
  color: rgba(200, 160, 80, 0.5);
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
  transition: color 0.2s;
}

.about-close:hover {
  color: rgba(200, 160, 80, 0.9);
}

.about-title {
  font-size: 22px;
  color: #e8d8b0;
  text-align: center;
  margin: 0 0 10px;
  letter-spacing: 4px;
}

.about-divider {
  width: 60px;
  height: 1px;
  margin: 0 auto 20px;
  background: linear-gradient(90deg, transparent, rgba(200, 160, 80, 0.5), transparent);
}

.about-content {
  color: rgba(200, 180, 150, 0.85);
  font-size: 14px;
  line-height: 1.8;
  text-align: center;
}

.about-content p {
  margin: 0 0 12px;
}

.about-motto {
  font-style: italic;
  color: rgba(200, 160, 80, 0.7);
  font-size: 15px;
  margin-top: 16px !important;
}

.about-credits {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid rgba(200, 160, 80, 0.12);
  text-align: center;
  font-size: 11px;
  color: rgba(200, 180, 140, 0.35);
  font-family: Arial, sans-serif;
  letter-spacing: 1px;
}

/* ============ 过渡动画 ============ */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
