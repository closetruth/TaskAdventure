<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import TaskTicker from './components/TaskTicker.vue'

const API_BASE_URL = 'http://localhost:8080'

const tab = ref('tasks')
const tasks = ref([])
const wallet = ref({ gold: 0, diamonds: 0 })
const currentTaskId = ref(null)
const currentTask = ref(null)
const message = ref('')
const newTaskTitle = ref('')

// reward ticker state (client-only)
const rewardEvents = ref([]) // recent reward popups

const weekSummary = ref(null)
const seasonMsg = ref('')
const acMsg = ref('')
const acGame = ref(null)
const selectedUnitId = ref(null)

let timer = null

const status = computed(() => currentTask.value?.status ?? 'IDLE')

const activeTasks = computed(() => tasks.value.filter(t => t.status !== 'FINISHED'))

const finishedTasks = computed(() =>
  tasks.value
    .filter(t => t.status === 'FINISHED')
    .slice()
    .sort((a, b) => {
      const ta = a.completedAt ? new Date(a.completedAt).getTime() : 0
      const tb = b.completedAt ? new Date(b.completedAt).getTime() : 0
      return tb - ta
    }),
)

const canStart = computed(
  () =>
    currentTask.value &&
    (currentTask.value.status === 'CREATED' || currentTask.value.status === 'PAUSED'),
)
const canPause = computed(() => currentTask.value?.status === 'RUNNING')
const canResume = computed(() => currentTask.value?.status === 'PAUSED')
const canFinish = computed(() => currentTask.value && currentTask.value.status !== 'FINISHED')
const isFinishedSelected = computed(() => currentTask.value?.status === 'FINISHED')

const selectedId = computed(() => currentTaskId.value)

function formatInstant(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString()
}

// Reward roll: implements documented probabilities
function rewardRoll() {
  // ~42% no reward
  if (Math.random() < 0.42) return { gold: 0, diamonds: 0 }
  const gold = Math.floor(Math.random() * (50 - 5 + 1)) + 5 // 5..50 gold
  const diamonds = Math.random() < 0.10 ? 1 : 0
  return { gold, diamonds }
}

function doRewardTick() {
  if (!currentTask.value || currentTask.value.status !== 'RUNNING') return
  const roll = rewardRoll()
  if ((roll.gold || 0) > 0 || (roll.diamonds || 0) > 0) {
    const id = Date.now().toString() + '-' + Math.random().toString(36).slice(2, 6)
    const ev = {
      id,
      gold: roll.gold,
      diamonds: roll.diamonds,
      taskId: currentTaskId.value,
      ts: new Date().toISOString(),
    }
    // newest first
    rewardEvents.value.unshift(ev)
    if (rewardEvents.value.length > 6) rewardEvents.value.pop()

    // send accumulation to backend and update task state from response
    request(`/task/${currentTaskId.value}/tick?gold=${roll.gold || 0}&diamonds=${roll.diamonds || 0}`, { method: 'POST' })
      .then(data => {
        // update task in tasks list and currentTask
        const idx = tasks.value.findIndex(t => t.id === data.id)
        if (idx !== -1) {
          // merge server task response fields we care about
          tasks.value[idx] = { ...tasks.value[idx], ...data }
        }
        if (currentTaskId.value === data.id) {
          currentTask.value = tasks.value[idx] ?? data
          // ensure accumulators exist
          currentTask.value.accumulatedGold = currentTask.value.accumulatedGold ?? 0
          currentTask.value.accumulatedDiamonds = currentTask.value.accumulatedDiamonds ?? 0
        }
      })
      .catch(err => {
        // keep local accumulator as fallback if POST fails
        currentTask.value.accumulatedGold = (currentTask.value.accumulatedGold || 0) + (roll.gold || 0)
        currentTask.value.accumulatedDiamonds = (currentTask.value.accumulatedDiamonds || 0) + (roll.diamonds || 0)
      })

    // 仍保留弹窗提示，但不修改 wallet
    setTimeout(() => {
      const idx = rewardEvents.value.findIndex(x => x.id === id)
      if (idx !== -1) rewardEvents.value.splice(idx, 1)
    }, 3000)
  }
}

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE_URL}${path}`, options)
  if (!res.ok) {
    let errorBody = await res.text()
    try {
      const j = JSON.parse(errorBody)
      if (j && typeof j.error === 'string') {
        errorBody = j.error
      }
    } catch {
      /* plain text */
    }
    throw new Error(errorBody || '请求失败')
  }
  return res.json()
}

function updateCurrentFromList() {
  if (!currentTaskId.value) return
  const task = tasks.value.find(item => item.id === currentTaskId.value)
  if (task) {
    // ensure client-only accumulator fields exist
    task.accumulatedGold = task.accumulatedGold ?? 0
    task.accumulatedDiamonds = task.accumulatedDiamonds ?? 0
    task.elapsedSeconds = task.elapsedSeconds ?? 0

    currentTask.value = task
  } else {
    currentTask.value = null
  }
}

function selectTask(task) {
  console.log('selectTask clicked:', task?.id)
  currentTaskId.value = Number(task.id)
  // ensure accumulators exist
  task.accumulatedGold = task.accumulatedGold ?? 0
  task.accumulatedDiamonds = task.accumulatedDiamonds ?? 0
  currentTask.value = task
  message.value = `已选中任务 ${task.id} · ${task.title}`
  try {
    document.title = `Selected ${task.id}`
  } catch (e) {
    // ignore (server-side rendering or no document)
  }
  if (task.status === 'RUNNING') {
    startTimer()
  } else {
    stopTimer()
  }
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    if (currentTask.value?.status === 'RUNNING') {
      currentTask.value.elapsedSeconds += 1

      // Per requirement: according to the displayed seconds, every 10 seconds
      if (currentTask.value.elapsedSeconds > 0 && currentTask.value.elapsedSeconds % 10 === 0) {
        doRewardTick()
      }
    }
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function loadWallet() {
  wallet.value = await request('/wallet')
}

async function loadTasks() {
  tasks.value = await request('/tasks')
  // ensure per-task client accumulators exist
  for (const t of tasks.value) {
    t.accumulatedGold = t.accumulatedGold ?? 0
    t.accumulatedDiamonds = t.accumulatedDiamonds ?? 0
    t.elapsedSeconds = t.elapsedSeconds ?? 0
  }
  updateCurrentFromList()
  if (currentTask.value?.status === 'RUNNING') {
    startTimer()
  } else {
    stopTimer()
  }
}

async function createTask() {
  const title = newTaskTitle.value.trim() || `任务 ${tasks.value.length + 1}`
  const task = await request(`/task?title=${encodeURIComponent(title)}`, { method: 'POST' })
  message.value = `任务已创建`
  newTaskTitle.value = ''
  await loadTasks()
  selectTask(tasks.value.find(t => t.id === task.id) ?? task)
}

async function startSelectedTask() {
  if (!currentTaskId.value || !canStart.value) return
  const task = await request(`/task/${currentTaskId.value}/start`, { method: 'POST' })
  Object.assign(currentTask.value, task)
  message.value = `已开始：${task.title}`
  startTimer()
  await loadTasks()
}

async function pauseTask() {
  if (!currentTaskId.value || !canPause.value) return
  const task = await request(`/task/${currentTaskId.value}/pause`, { method: 'POST' })
  Object.assign(currentTask.value, task)
  message.value = `已暂停`
  stopTimer()
  await loadTasks()
}

async function resumeTask() {
  if (!currentTaskId.value || !canResume.value) return
  const task = await request(`/task/${currentTaskId.value}/resume`, { method: 'POST' })
  Object.assign(currentTask.value, task)
  message.value = '已继续'
  startTimer()
  await loadTasks()
}

async function finishTask() {
  if (!currentTaskId.value || !canFinish.value) return
  const task = await request(`/task/${currentTaskId.value}/finish`, { method: 'POST' })
  Object.assign(currentTask.value, task)
  const loot =
    task.finishGoldAwarded > 0 || task.finishDiamondAwarded > 0
      ? `掉落：金币 +${task.finishGoldAwarded}，钻石 +${task.finishDiamondAwarded}`
      : '本次无金币/钻石掉落'

  message.value = `${loot}\n完成时间：${formatInstant(task.completedAt)}`

  stopTimer()
  await loadTasks()

  await loadWallet()
}

async function loadWeekSummary() {
  seasonMsg.value = ''
  weekSummary.value = await request('/season/week')
  if (weekSummary.value?.wallet) {
    wallet.value = weekSummary.value.wallet
  }
}

async function claimWeek() {
  seasonMsg.value = ''
  const res = await request('/season/week/claim', { method: 'POST' })
  wallet.value = res.wallet
  seasonMsg.value = res.message
  await loadWeekSummary()
}

async function loadAcGame() {
  acMsg.value = ''
  selectedUnitId.value = null
  const data = await request('/autochess/game')
  acGame.value = data
  if (data.wallet) {
    wallet.value = data.wallet
  }
}

async function acPost(path, body) {
  acMsg.value = ''
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  let msg = ''
  if (!res.ok) {
    let errorBody = await res.text()
    try {
      const j = JSON.parse(errorBody)
      if (j && typeof j.error === 'string') {
        errorBody = j.error
      }
    } catch {
      /* plain */
    }
    acMsg.value = errorBody || '请求失败'
    return
  }
  const data = await res.json()
  acGame.value = data
  if (data.wallet) {
    wallet.value = data.wallet
  }
  acMsg.value = data.message || msg
}

async function acRefresh() {
  await acPost('/autochess/shop/refresh')
}

async function acBuy(slot) {
  await acPost('/autochess/shop/buy', { slot })
}

async function acFight() {
  await acPost('/autochess/round/fight')
}

async function acReset() {
  selectedUnitId.value = null
  await acPost('/autochess/run/reset')
}

async function acRevive() {
  await acPost('/autochess/run/revive')
}

async function acUnplace(boardIndex) {
  await acPost('/autochess/unit/unplace', { boardIndex })
}

async function acUpgrade(unitId) {
  await acPost('/autochess/unit/upgrade', { unitId })
}

async function acSell(unitId) {
  await acPost('/autochess/unit/sell', { unitId })
}

async function acMerge(unitId) {
  await acPost('/autochess/unit/merge', { unitId })
}

function starLabel(n) {
  const s = Number(n) || 1
  return '★'.repeat(Math.min(3, Math.max(1, s)))
}

function selectUnitForPlace(id) {
  if (acGame.value?.game?.gameOver) {
    return
  }
  selectedUnitId.value = selectedUnitId.value === id ? null : id
}

async function onBoardClick(boardIndex) {
  const g = acGame.value?.game
  if (!g || g.gameOver) {
    return
  }
  const uid = selectedUnitId.value
  if (!uid) {
    return
  }
  if (g.board[boardIndex]) {
    return
  }
  await acPost('/autochess/unit/place', { unitId: uid, boardIndex })
  selectedUnitId.value = null
}

const isSelected = (task) => Number(task.id) === Number(currentTaskId.value)

function formatDuration(seconds) {
  const s = Number(seconds) || 0
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return [h, m, sec].map(v => v.toString().padStart(2, '0')).join(':')
}

watch(tab, async value => {
  if (value === 'season') {
    await loadWeekSummary()
  }
  if (value === 'autochess') {
    await loadAcGame()
  }
})

onMounted(async () => {
  await Promise.all([loadTasks(), loadWallet()])
})
onUnmounted(stopTimer)
</script>

<template>
  <div class="page">
    <header class="top-bar">
      <div class="brand">🎮 Gamified Task</div>
      <div class="wallet">
        <span class="coin">🪙 金币 {{ wallet.gold }}</span>
        <span class="gem">💎 钻石 {{ wallet.diamonds }}</span>
      </div>
    </header>

    <nav class="tabs" aria-label="主功能">
      <button type="button" class="tab" :class="{ active: tab === 'tasks' }" @click="tab = 'tasks'">
        任务
      </button>
      <button type="button" class="tab" :class="{ active: tab === 'autochess' }" @click="tab = 'autochess'">
        迷你自走棋
      </button>
      <button type="button" class="tab" :class="{ active: tab === 'season' }" @click="tab = 'season'">
        本周结算
      </button>
    </nav>

    <div v-show="tab === 'tasks'" class="container">
      <!-- 左：任务列表 -->
      <div class="task-box">
        <h2>📦 任务</h2>
        <div class="new-task">
          <input v-model="newTaskTitle" placeholder="输入任务名" />
          <button type="button" class="btn" @click="createTask">+ 新任务</button>
        </div>

        <h3 class="section-title">进行中</h3>
        <p v-if="activeTasks.length === 0" class="empty">暂无进行中的任务，先新建或从下方历史里看已完成。</p>
        <div
          v-for="task in activeTasks"
          :key="task.id"
          class="task-item"
          :class="{ selected: isSelected(task) }"
          role="button"
          tabindex="0"
          @click.stop="selectTask(task)"
          @keydown.enter.prevent="selectTask(task)"
        >
          <div class="title">{{ task.title }}</div>
          <div class="status">状态：{{ task.status }}</div>
          <div class="pending">待领取：🪙 +{{ task.accumulatedGold ?? 0 }} · 💎 +{{ task.accumulatedDiamonds ?? 0 }}</div>
          <div class="times">
            <div>创建：{{ formatInstant(task.createdAt) }}</div>
          </div>
          <div class="hint">点击选中，在右侧开始 / 暂停 / 完成</div>
        </div>

        <h3 class="section-title done-heading">已完成</h3>
        <p v-if="finishedTasks.length === 0" class="empty">还没有完成的任务。</p>
        <div
          v-for="task in finishedTasks"
          :key="task.id"
          class="task-item done"
          :class="{ selected: isSelected(task) }"
          role="button"
          tabindex="0"
          @click.stop="selectTask(task)"
          @keydown.enter.prevent="selectTask(task)"
        >
          <div class="title">{{ task.title }}</div>
          <div class="loot">
            掉落：金币 +{{ task.finishGoldAwarded }} · 钻石 +{{ task.finishDiamondAwarded }}
          </div>
          <div class="times">
            <div>创建：{{ formatInstant(task.createdAt) }}</div>
            <div>完成：{{ formatInstant(task.completedAt) }}</div>
            <div>总耗时：{{ formatDuration(task.elapsedSeconds) }}</div>
          </div>
        </div>
      </div>

      <!-- 右：操作与说明 -->
      <div class="panel">
        <h2>⚡ 当前选中</h2>

        <div v-if="!currentTask" class="muted">请在左侧点击选择一个任务。</div>

        <template v-else>
          <div>状态：{{ status }}</div>
          <div>任务：{{ currentTask.title }}</div>

          <div class="pending selected-pending">待领取：🪙 +{{ currentTask.accumulatedGold ?? 0 }} · 💎 +{{ currentTask.accumulatedDiamonds ?? 0 }}</div>

          <div class="elapsed">运行时间 {{ formatDuration(currentTask.elapsedSeconds ?? 0) }}</div>

          <div class="bar">
            <div
              class="bar-inner"
              :style="{ width: Math.min((currentTask.elapsedSeconds % 60) / 60 * 100, 100) + '%' }"
            ></div>
          </div>

          <div v-if="isFinishedSelected" class="banner-done">该任务已完成，仅可查看结算信息。</div>

          <div class="controls">
            <button type="button" :disabled="!canStart" @click="startSelectedTask">▶ 开始</button>
            <button type="button" :disabled="!canPause" @click="pauseTask">⏸ 暂停</button>
            <button type="button" :disabled="!canResume" @click="resumeTask">▶ 继续</button>
            <button type="button" :disabled="!canFinish" @click="finishTask">✔ 完成</button>
          </div>

          <TaskTicker v-if="rewardEvents.length" :events="rewardEvents" />
        </template>

        <p class="message" v-if="message">{{ message }}</p>

        <section class="rules" aria-labelledby="rules-heading">
          <h2 id="rules-heading">📐 任务与奖励说明</h2>
          <ul>
            <li>
              <strong>间歇掉落</strong>：选中并运行的任务每 <strong>10 秒</strong> 会进行一次掉落判定（客户端），如果命中会把掉落累积到该任务上（显示为 "待领取"）。
            </li>
            <li>
              <strong>进度条</strong>：进度条每 <strong>60 秒</strong> 循环走完一圈。
            </li>
            <li>
              <strong>领取</strong>：只有在点击 <em>完成</em> 后，累积的掉落才会真正加入钱包。
            </li>
            <li>
              <strong>完成奖励</strong>：点击完成时，除了累积掉落，还会额外获得一份随机奖励（金币 5～50，10% 概率获得 1 钻石）。
            </li>
            <li><strong>记录</strong>：系统记录任务的创建时间、完成时间以及任务的总专注耗时。</li>
          </ul>
        </section>
      </div>
    </div>

    <section v-show="tab === 'autochess'" class="ac-root" aria-label="迷你自走棋">
      <div class="ac-intro">
        <h2>♟ 迷你自走棋</h2>
        <p class="game-lead">
          <strong>商店</strong>高费棋子随回合略变多；<strong>棋盘</strong>上同羁绊≥2 枚有战力加成；<strong>备战席</strong>两枚<strong>同名同星级</strong>可合成升星（最高
          ★★★）。连胜加金币，连败受伤略增。用任务金币买子，钻石可强化或复活。
        </p>
      </div>

      <div v-if="acGame?.game" class="ac-preview">
        <div class="ac-preview-row">
          <span>下一波：<strong>{{ acGame.game.nextEnemyName || '…' }}</strong></span>
          <span class="ac-preview-sub">敌战力约 {{ acGame.game.nextEnemyPowerBase }} ±5（掷骰后对比）</span>
        </div>
        <div class="ac-preview-syn">{{ acGame.game.synergySummary }}</div>
      </div>

      <div v-if="acGame?.game" class="ac-toolbar">
        <div class="ac-stats">
          第 <strong>{{ acGame.game.round }}</strong> 轮 · 生命
          <strong>{{ acGame.game.playerHp }}</strong>
          <span class="ac-streak" v-if="!acGame.game.gameOver">
            · 连胜 {{ acGame.game.winStreak ?? 0 }} / 连败 {{ acGame.game.loseStreak ?? 0 }}
          </span>
          <span v-if="acGame.game.gameOver" class="ac-over">（本局结束）</span>
        </div>
        <div class="ac-toolbar-btns">
          <button type="button" @click="acFight" :disabled="acGame.game.gameOver">⚔ 开始战斗</button>
          <button type="button" @click="acRefresh" :disabled="acGame.game.gameOver">刷新商店（-3 金）</button>
          <button type="button" @click="acReset">重开新局</button>
          <button type="button" @click="acRevive" :disabled="!acGame.game.gameOver">复活（-5 钻）</button>
        </div>
      </div>

      <div v-if="acGame?.game" class="ac-shop-block">
        <h3 class="ac-h3">商店</h3>
        <div class="ac-shop-slots">
          <div
            v-for="(offer, idx) in acGame.game.shop"
            :key="'shop-' + idx"
            class="ac-card"
            :class="{ 'ac-card-empty': !offer }"
          >
            <template v-if="offer">
              <div class="ac-card-title">{{ offer.name }}</div>
              <div class="ac-trait">{{ offer.trait || '中立' }}</div>
              <div class="ac-card-meta">T{{ offer.tier }} · 攻{{ offer.atk }} 血{{ offer.hp }}</div>
              <div class="ac-card-cost">{{ offer.cost }} 金</div>
              <button type="button" @click="acBuy(idx)" :disabled="acGame.game.gameOver">购买</button>
            </template>
            <template v-else>空位</template>
          </div>
        </div>
      </div>

      <div v-if="acGame?.game" class="ac-board-block">
        <h3 class="ac-h3">棋盘</h3>
        <div class="ac-board">
          <div
            v-for="(cell, bi) in acGame.game.board"
            :key="'cell-' + bi"
            class="ac-cell"
            :class="{
              filled: !!cell,
              'can-place': selectedUnitId && !cell && !acGame.game.gameOver,
            }"
            role="button"
            tabindex="0"
            @click="onBoardClick(bi)"
            @keydown.enter.prevent="onBoardClick(bi)"
          >
            <template v-if="cell">
              <div class="ac-card-title">{{ cell.name }} {{ starLabel(cell.stars) }}</div>
              <div class="ac-trait">{{ cell.trait || '中立' }}</div>
              <div class="ac-card-meta">攻{{ cell.atk }} · {{ cell.currentHp }}/{{ cell.maxHp }}</div>
              <div class="ac-cell-actions">
                <button type="button" @click.stop="acUnplace(bi)">下阵</button>
                <button type="button" @click.stop="acUpgrade(cell.id)">强化(-1钻)</button>
                <button type="button" @click.stop="acSell(cell.id)">卖</button>
              </div>
            </template>
            <template v-else>
              {{ selectedUnitId ? '点击上阵' : '空 #' + (bi + 1) }}
            </template>
          </div>
        </div>
      </div>

      <div v-if="acGame?.game" class="ac-bench-block">
        <h3 class="ac-h3">备战席（{{ acGame.game.bench?.length ?? 0 }} / 8）</h3>
        <p v-if="selectedUnitId" class="ac-hint">
          已选择棋子，点棋盘空位上阵。
          <button type="button" class="linkish" @click="selectedUnitId = null">取消</button>
        </p>
        <div class="ac-bench-list">
          <div
            v-for="u in acGame.game.bench"
            :key="u.id"
            class="ac-card ac-bench-card"
            :class="{ selected: selectedUnitId === u.id }"
            role="button"
            tabindex="0"
            @click="selectUnitForPlace(u.id)"
            @keydown.enter.prevent="selectUnitForPlace(u.id)"
          >
            <div class="ac-card-title">{{ u.name }} {{ starLabel(u.stars) }}</div>
            <div class="ac-trait">{{ u.trait || '中立' }}</div>
            <div class="ac-card-meta">攻{{ u.atk }} · {{ u.currentHp }}/{{ u.maxHp }}</div>
            <div class="ac-cell-actions">
              <button type="button" @click.stop="acMerge(u.id)" :disabled="(u.stars ?? 1) >= 3">合成</button>
              <button type="button" @click.stop="acUpgrade(u.id)">强化</button>
              <button type="button" @click.stop="acSell(u.id)">卖</button>
            </div>
          </div>
        </div>
      </div>

      <p v-if="acGame?.game?.lastLog" class="ac-log">{{ acGame.game.lastLog }}</p>
      <p v-if="acMsg" class="game-msg">{{ acMsg }}</p>
    </section>

    <section v-show="tab === 'season'" class="game-panel" aria-label="本周排位结算">
      <h2>📅 本周结算（自然周）</h2>
      <p class="game-lead">
        按系统时区的<strong>周一 0 点 ~ 下周一 0 点</strong>统计<strong>已完成任务</strong>的专注时长与件数，给出段位称号与可领取的一次性金币（每自然周仅可领一次）。
      </p>
      <div v-if="weekSummary" class="season-card">
        <div>周次：<strong>{{ weekSummary.weekKey }}</strong>（{{ weekSummary.weekRangeLabel }}）</div>
        <div>时区：{{ weekSummary.zoneId }}</div>
        <div>本周完成任务：{{ weekSummary.tasksCompletedThisWeek }} 件</div>
        <div>本周专注（已完成任务合计）：{{ weekSummary.focusMinutesThisWeek }} 分钟</div>
        <div>
          段位：<strong>T{{ weekSummary.rankTier }}</strong> · {{ weekSummary.rankTitle }}
        </div>
        <div>预计结算金币：{{ weekSummary.settlementGoldPreview }}（领取时入账）</div>
        <div :class="weekSummary.claimedThisWeek ? 'claimed' : 'unclaimed'">
          {{ weekSummary.claimedThisWeek ? '本周奖励已领取' : '本周奖励尚未领取' }}
        </div>
        <button
          type="button"
          class="claim-btn"
          :disabled="weekSummary.claimedThisWeek"
          @click="claimWeek"
        >
          领取本周结算
        </button>
      </div>
      <p v-else class="muted">加载中…</p>
      <p v-if="seasonMsg" class="game-msg">{{ seasonMsg }}</p>
    </section>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #0c0c0c;
  color: #f4f4f4;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
    monospace;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 2px solid #2a2a2a;
  background: #111;
}

.brand {
  font-weight: 700;
  letter-spacing: 0.02em;
}

.wallet {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.coin {
  color: #f8d66d;
}

.gem {
  color: #7ecbff;
}

.tabs {
  display: flex;
  gap: 4px;
  padding: 8px 16px 0;
  background: #0f0f0f;
  border-bottom: 1px solid #2a2a2a;
}

.tab {
  padding: 10px 14px;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  background: transparent;
  color: #aaa;
  cursor: pointer;
}

.tab:hover {
  color: #e8e8e8;
  background: #1a1a1a;
}

.tab.active {
  color: #f4f4f4;
  background: #141414;
  border-color: #2a2a2a;
  border-bottom-color: #141414;
  margin-bottom: -1px;
}

.game-panel {
  padding: 20px 24px 48px;
  max-width: 720px;
}

.game-panel h2 {
  margin: 0 0 10px;
  font-size: 18px;
}

.game-lead {
  font-size: 13px;
  line-height: 1.55;
  color: #b0b0b0;
  margin: 0 0 16px;
}

.game-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.game-msg {
  color: #f8d66d;
  margin: 8px 0 0;
  font-size: 14px;
}

.game-detail {
  color: #9adcc4;
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.45;
}

.season-card {
  border: 1px solid #333;
  border-radius: 8px;
  padding: 16px;
  background: #121212;
  font-size: 14px;
  line-height: 1.7;
}

.season-card .claimed {
  color: #7ecbff;
  margin: 8px 0;
}

.season-card .unclaimed {
  color: #f8d66d;
  margin: 8px 0;
}

.claim-btn {
  margin-top: 12px;
}

.ac-root {
  padding: 20px 24px 48px;
  max-width: 960px;
}

.ac-preview {
  margin-bottom: 14px;
  padding: 12px 14px;
  background: #101818;
  border: 1px solid #2a3d34;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.ac-preview-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px 16px;
  margin-bottom: 6px;
}

.ac-preview-sub {
  color: #8a9a90;
  font-size: 12px;
}

.ac-preview-syn {
  color: #9adcc4;
  font-size: 12px;
}

.ac-streak {
  color: #c9a227;
  font-size: 13px;
}

.ac-trait {
  font-size: 11px;
  color: #7ecbff;
  margin-bottom: 4px;
}

.ac-intro h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.ac-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px 14px;
  background: #121212;
  border: 1px solid #2e2e2e;
  border-radius: 8px;
}

.ac-stats {
  font-size: 15px;
}

.ac-over {
  color: #ff8a8a;
  margin-left: 6px;
}

.ac-toolbar-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ac-h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #9a9a9a;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ac-shop-block,
.ac-board-block,
.ac-bench-block {
  margin-bottom: 22px;
}

.ac-shop-slots,
.ac-board,
.ac-bench-list {
  display: grid;
  gap: 10px;
}

.ac-shop-slots {
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
}

.ac-board {
  grid-template-columns: repeat(4, 1fr);
}

.ac-bench-list {
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
}

.ac-card {
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  padding: 10px;
  background: #161616;
  font-size: 12px;
  line-height: 1.45;
}

.ac-card-empty {
  opacity: 0.55;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  color: #666;
}

.ac-card-title {
  font-weight: 700;
  margin-bottom: 4px;
  color: #eee;
}

.ac-card-meta {
  color: #9a9a9a;
  margin-bottom: 6px;
}

.ac-card-cost {
  color: #f8d66d;
  margin-bottom: 8px;
}

.ac-cell {
  min-height: 110px;
  border: 1px dashed #444;
  border-radius: 8px;
  padding: 8px;
  cursor: default;
  background: #101010;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.ac-cell.filled {
  border-style: solid;
  border-color: #4a6a5a;
  background: #121a16;
}

.ac-cell.can-place {
  border-color: #5ee4a8;
  cursor: pointer;
  background: #0f1a14;
}

.ac-cell-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.ac-cell-actions button {
  font-size: 11px;
  padding: 4px 6px;
}

.ac-bench-card {
  cursor: pointer;
  text-align: left;
}

.ac-bench-card.selected {
  border-color: #5ee4a8;
  box-shadow: 0 0 0 1px rgba(94, 228, 168, 0.25);
}

.ac-hint {
  font-size: 13px;
  color: #7ecbff;
  margin: 0 0 10px;
}

.linkish {
  margin-left: 8px;
  background: none;
  border: none;
  color: #7ecbff;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  font: inherit;
}

.ac-log {
  font-size: 13px;
  line-height: 1.55;
  color: #b8b8b8;
  padding: 12px 14px;
  background: #111;
  border-radius: 8px;
  border: 1px solid #2a2a2a;
  white-space: pre-wrap;
}

.container {
  display: flex;
  align-items: flex-start;
}

.task-box {
  width: 44%;
  max-width: 520px;
  border-right: 2px solid #2a2a2a;
  padding: 20px;
  max-height: calc(100vh - 56px);
  overflow-y: auto;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: #9a9a9a;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.done-heading {
  margin-top: 24px;
}

.empty {
  font-size: 13px;
  color: #777;
  margin: 0 0 8px;
}

.task-item {
  border: 1px solid #3a3a3a;
  margin: 10px 0;
  padding: 12px;
  border-radius: 6px;
  background: #141414;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.task-item:hover {
  border-color: #666;
  background: #181818;
}

.task-item.selected {
  border-color: #5ee4a8;
  box-shadow: 0 0 0 1px rgba(94, 228, 168, 0.25);
}

.task-item.done {
  opacity: 0.92;
  background: #101818;
}

.hint {
  font-size: 11px;
  color: #666;
  margin-top: 8px;
}

.title {
  font-weight: bold;
  margin-bottom: 6px;
}

.status {
  color: #5ee4a8;
  margin-bottom: 4px;
}

.reward {
  font-size: 13px;
  color: #c8c8c8;
  margin: 4px 0;
}

.reward.subtle {
  color: #888;
  font-size: 12px;
}

.times {
  font-size: 12px;
  color: #9a9a9a;
  margin: 8px 0;
  line-height: 1.5;
}

.loot {
  font-size: 13px;
  color: #f8d66d;
  margin: 6px 0;
}

.panel {
  flex: 1;
  padding: 20px 24px 40px;
  max-height: calc(100vh - 56px);
  overflow-y: auto;
}

.muted {
  color: #888;
  margin-bottom: 12px;
}

.breakdown {
  font-size: 13px;
  color: #b0b0b0;
  margin-top: 6px;
}

.energy {
  font-size: 28px;
  margin: 12px 0 8px;
  color: #e8e8e8;
}

.bar {
  height: 14px;
  border: 1px solid #444;
  border-radius: 4px;
  margin-bottom: 12px;
  overflow: hidden;
  background: #1a1a1a;
}

.bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #5ee4a8, #7ecbff);
}

.banner-done {
  font-size: 13px;
  color: #7ecbff;
  margin: 8px 0 12px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.controls button {
  margin: 0;
}

.message {
  margin-top: 14px;
  color: #f8d66d;
  white-space: pre-wrap;
  line-height: 1.5;
}

.rules {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #2a2a2a;
  font-size: 13px;
  line-height: 1.55;
  color: #c4c4c4;
}

.rules h2 {
  font-size: 15px;
  margin: 0 0 8px;
  color: #e8e8e8;
}

.rules-intro {
  margin: 0 0 12px;
  color: #888;
}

.rules ul {
  margin: 0;
  padding-left: 1.2rem;
}

.rules li {
  margin: 8px 0;
}

.rules code {
  font-size: 12px;
  background: #1a1a1a;
  padding: 1px 5px;
  border-radius: 3px;
  border: 1px solid #333;
}

.new-task {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

input {
  flex: 1;
  background: #0a0a0a;
  color: #fff;
  border: 1px solid #444;
  border-radius: 4px;
  padding: 8px 10px;
}

button {
  background: #1a1a1a;
  color: #fff;
  border: 1px solid #555;
  border-radius: 4px;
  padding: 6px 12px;
  cursor: pointer;
}

button:hover:not(:disabled) {
  background: #2a2a2a;
}

button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.elapsed {
  font-size: 13px;
  color: #9adcc4;
  margin-top: 6px;
}

.pending {
  font-size: 13px;
  color: #f8d66d;
  margin-top: 6px;
}
.selected-pending { color: #f8d66d; font-weight: 700; margin-top: 8px }
</style>
