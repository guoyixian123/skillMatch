<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="nb-card stat-card">
        <div class="stat-top">
          <span class="material-symbols-outlined stat-icon bg-yellow">groups</span>
          <span class="stat-badge up">+12% UP</span>
        </div>
        <p class="stat-label">总用户数</p>
        <h2 class="stat-value">{{ fmtNum(data.totalUsers) }}</h2>
      </div>
      <div class="nb-card stat-card">
        <div class="stat-top">
          <span class="material-symbols-outlined stat-icon bg-blue">person</span>
          <span class="stat-badge up">+8% ↑</span>
        </div>
        <p class="stat-label">真人用户</p>
        <h2 class="stat-value">{{ fmtNum(data.realUsers) }}</h2>
      </div>
      <div class="nb-card stat-card">
        <div class="stat-top">
          <span class="material-symbols-outlined stat-icon bg-magenta">smart_toy</span>
          <span class="stat-badge stable">稳定</span>
        </div>
        <p class="stat-label">机器人</p>
        <h2 class="stat-value">{{ fmtNum(data.robotUsers) }}</h2>
      </div>
      <div class="stat-card stat-export">
        <span class="material-symbols-outlined" style="font-size:64px;">description</span>
        <span class="export-title">导出Excel</span>
        <p class="export-desc">完整数据导出</p>
      </div>
    </div>

    <!-- 中排：地图 + Top城市 -->
    <div class="mid-grid">
      <!-- 中国地图 — 与原型一致的静态地图 + CSS 标记 -->
      <div class="nb-card map-card">
        <div class="card-header">
          <div>
            <h3>用户分布密度</h3>
            <p>全国用户实时分布热力</p>
          </div>
          <div class="live-badge">{{ mappedCities.length ? mappedCities.length + '城' : '暂无' }}</div>
        </div>
        <div class="map-area">
          <div class="map-canvas">
            <!-- 中国地图底图（ECharts 渲染） -->
            <v-chart :option="mapBaseOption" autoresize style="width:100%;height:100%;" :key="mapKey" v-if="mapGeoReady" />
            <!-- 仅在地图和标记都没就绪时才显示加载中 -->
            <div class="map-loading" v-if="!mapGeoReady && !mapReady">
              <span class="material-symbols-outlined spin-icon">public</span>
              <span>加载地图中...</span>
            </div>
            <!-- CSS 叠加的 Brutalism 标记点 -->
            <div class="map-markers" v-if="mapReady && mapMarkers.length">
              <div
                v-for="pt in mapMarkers"
                :key="pt.name"
                class="map-marker"
                :class="pt.style"
                :style="{ left: pt.left + '%', top: pt.top + '%' }"
                :title="pt.name + ' ' + fmtK(pt.count) + '人'"
              >
                <span class="marker-label" v-if="pt.showLabel">{{ pt.name }}</span>
              </div>
            </div>
            <!-- 无城市数据提示 -->
            <div class="map-empty" v-if="mapReady && !mapMarkers.length">
              <span class="material-symbols-outlined" style="font-size:36px;opacity:0.3;">location_off</span>
              <span>暂无城市分布数据</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Top 10 城市柱状图 -->
      <div class="nb-card city-card">
        <div class="card-header">
          <div>
            <h3>Top 城市</h3>
            <p>用户城市分布</p>
          </div>
        </div>
        <div class="city-bars" v-if="topCities.length">
          <div v-for="c in topCities" :key="c.city" class="city-bar-group">
            <div class="city-bar-label">
              <span>{{ c.city }}</span>
              <span class="city-bar-nums">👤{{ fmtK(c.realCount) }} 🤖{{ fmtK(c.robotCount) }}</span>
            </div>
            <div class="city-bar-track">
              <div class="city-bar-real" :style="{ width: c.realPct + '%' }"></div>
              <div class="city-bar-robot" :style="{ width: c.robotPct + '%' }"></div>
            </div>
          </div>
          <div class="city-legend">
            <span><span class="legend-dot bg-yellow"></span> 真人</span>
            <span><span class="legend-dot bg-magenta"></span> 机器人</span>
          </div>
        </div>
        <div class="city-bars city-empty" v-else>
          <span class="material-symbols-outlined" style="font-size:36px;opacity:0.3;">bar_chart_4_bars</span>
          <span>暂无城市分布数据</span>
        </div>
      </div>
    </div>

    <!-- 底排：饼图 + 折线图 + 状态 -->
    <div class="bottom-grid">
      <!-- 用户占比 -->
      <div class="nb-card pie-card">
        <h3 class="pie-title">用户占比</h3>
        <v-chart :option="userRatioOption" autoresize style="height:200px" />
        <div class="pie-legend-bar">
          <span>真人: {{ fmtK(data.realUsers) }}</span>
          <span>机器人: {{ fmtK(data.robotUsers) }}</span>
        </div>
      </div>

      <!-- 7日增长 -->
      <div class="nb-card pie-card">
        <h3 class="pie-title">近7日增长</h3>
        <v-chart :option="dailyOption" autoresize style="height:220px" />
      </div>

      <!-- 用户技能 & 爱好总览 -->
      <div class="nb-card pie-card">
        <h3 class="pie-title">能教的技能</h3>
        <v-chart :option="canSkillOption" autoresize style="height:160px" />
        <div class="tag-list">
          <span class="tag-item" v-for="t in canSkillTags" :key="t.name">
            <span class="tag-dot" style="background:#0040E0"></span>
            {{ t.name }} <span class="tag-count">{{ t.count }}</span>
          </span>
        </div>
      </div>

      <div class="nb-card pie-card">
        <h3 class="pie-title">想学的技能</h3>
        <v-chart :option="wantSkillOption" autoresize style="height:160px" />
        <div class="tag-list">
          <span class="tag-item" v-for="t in wantSkillTags" :key="t.name">
            <span class="tag-dot" style="background:#B1008C"></span>
            {{ t.name }} <span class="tag-count">{{ t.count }}</span>
          </span>
        </div>
      </div>

      <div class="nb-card pie-card">
        <h3 class="pie-title">兴趣爱好</h3>
        <v-chart :option="hobbyOption" autoresize style="height:160px" />
        <div class="tag-list">
          <span class="tag-item" v-for="t in hobbyTags" :key="t.name">
            <span class="tag-dot" style="background:#4ADE80"></span>
            {{ t.name }} <span class="tag-count">{{ t.count }}</span>
          </span>
        </div>
      </div>

      <!-- 安全状态 -->
      <div class="nb-card pie-card">
        <h3 class="pie-title">账户安全</h3>
        <v-chart :option="securityOption" autoresize style="height:180px" />
        <div class="security-cards">
          <div class="sec-card bg-yellow"><span>正常</span><strong>{{ fmtPct(data.normalUsers, data.totalUsers) }}%</strong></div>
          <div class="sec-card bg-red"><span>冻结</span><strong>{{ fmtPct(data.frozenUsers, data.totalUsers) }}%</strong></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard } from '@/api/admin'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'

const data = ref({})
const mapReady = ref(false)
const mapGeoReady = ref(false)
const mapKey = ref(0)

// 中国主要城市经纬度
const cityCoords = {
  '北京':[116.4,39.9],'上海':[121.5,31.2],'广州':[113.3,23.1],'深圳':[114.1,22.5],
  '杭州':[120.2,30.3],'成都':[104.1,30.7],'武汉':[114.3,30.6],'南京':[118.8,32.1],
  '重庆':[106.6,29.6],'天津':[117.2,39.1],'苏州':[120.6,31.3],'西安':[108.9,34.3],
  '长沙':[113,28.2],'郑州':[113.6,34.8],'青岛':[120.4,36.1],'沈阳':[123.4,41.8],
  '哈尔滨':[126.5,45.8],'昆明':[102.7,25],'福州':[119.3,26.1],'厦门':[118.1,24.5],
  '合肥':[117.3,31.8],'济南':[117,36.7],'南宁':[108.4,22.8],'贵阳':[106.6,26.6],
}

// 中国地图经纬度边界（用于百分比转换）
const MAP_BOUNDS = { minLng: 73, maxLng: 135, minLat: 17, maxLat: 54 }

/** 经纬度 → 地图容器内百分比坐标 */
function geoToPercent(lng, lat) {
  return {
    left: ((lng - MAP_BOUNDS.minLng) / (MAP_BOUNDS.maxLng - MAP_BOUNDS.minLng)) * 100,
    top: ((MAP_BOUNDS.maxLat - lat) / (MAP_BOUNDS.maxLat - MAP_BOUNDS.minLat)) * 100,
  }
}

function fmtNum(n) { return (n || 0).toLocaleString() }
function fmtK(n) { n = n || 0; return n >= 1000 ? (n/1000).toFixed(1)+'k' : String(n) }
function fmtPct(a, b) { return b > 0 ? Math.round((a||0)/b*100) : 0 }

// 统一数据源 — 过滤出有坐标的城市（兼容后端带"市"后缀）
const mappedCities = computed(() => {
  const cities = data.value.cityDistribution || []
  return cities
    .map(c => {
      // 后端可能返回"广州市"，前端 cityCoords key 是"广州"，去掉"市"后缀匹配
      let name = c.city || ''
      let xy = cityCoords[name]
      if (!xy && name.endsWith('市')) {
        xy = cityCoords[name.slice(0, -1)]
      }
      if (!xy) return null
      return {
        city: name,
        realCount: c.realCount || 0,
        robotCount: c.robotCount || 0,
        total: c.total || (c.realCount || 0) + (c.robotCount || 0),
        coords: xy,
      }
    })
    .filter(Boolean)
})

// Top 10 城市柱状图 — 与地图使用同一数据源
const topCities = computed(() => {
  const list = mappedCities.value.slice(0, 10)
  if (!list.length) return []
  const maxTotal = Math.max(...list.map(c => c.total), 1)
  return list.map(c => ({
    city: c.city, realCount: c.realCount, robotCount: c.robotCount,
    realPct: Math.round(c.realCount / maxTotal * 100),
    robotPct: Math.round(c.robotCount / maxTotal * 100),
  }))
})

// 地图散点数据 — 与柱状图使用同一数据源
const mapScatterData = computed(() => {
  return mappedCities.value.map(c => ({
    name: c.city, value: [...c.coords, c.total],
  }))
})

// 加载中国地图 GeoJSON（独立函数，不在 computed 内）
async function loadChinaMap() {
  const urls = ['/china.json', 'https://geojson.cn/api/data/china.json']
  for (const url of urls) {
    try {
      const res = await fetch(url)
      if (res.ok) {
        const geoJSON = await res.json()
        echarts.registerMap('china', geoJSON)
        mapGeoReady.value = true; mapReady.value = true
        mapKey.value++
        console.log('✅ 中国地图加载成功:', url)
        return
      }
    } catch (e) { console.warn('地图源失败:', url, e.message) }
  }
  console.warn('⚠️ 所有地图源均不可用')
  mapReady.value = true
  mapKey.value++
}

// CSS 标记点 — 与原型 Brutalism 风格一致
// 颜色含义: 黄色=人多  蓝色=中等  品红=人少
const mapMarkers = computed(() => {
  const pts = mapScatterData.value
  if (!pts.length) return []
  const maxCount = Math.max(...pts.map(p => p.value[2]), 1)
  return pts.map((p, i) => {
    const pos = geoToPercent(p.value[0], p.value[1])
    const ratio = p.value[2] / maxCount
    // 按总人数分色
    let style = 'marker-lg'       // 黄色：人数多 (>=60%)
    if (ratio < 0.3) style = 'marker-sm'      // 品红：人数少 (<30%)
    else if (ratio < 0.6) style = 'marker-md' // 蓝色：中等 (30%-60%)
    // 前3大城市用 ping 动画
    if (i < 3) style += ' marker-ping'
    const showLabel = ratio >= 0.3 || i < 5
    return {
      name: p.name,
      count: p.value[2],
      left: pos.left,
      top: pos.top,
      style,
      showLabel,
    }
  })
})

// ECharts 底图 — 只渲染中国轮廓（与原型灰色基调一致）
const mapBaseOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { show: false },
  geo: {
    map: 'china',
    roam: false,
    zoom: 1.2,
    center: [104.5, 36],
    aspectScale: 0.85,
    label: { show: false },
    itemStyle: {
      areaColor: '#e5e7eb',
      borderColor: '#1b1b1b',
      borderWidth: 2,
    },
    emphasis: {
      itemStyle: { areaColor: '#d4d4d8' },
      label: { show: false },
    },
  },
  series: [],
}))

// ECharts 图表选项 — 用户占比饼图
const userRatioOption = computed(() => ({
  tooltip:{trigger:'item'}, series:[{ type:'pie', radius:['50%','80%'],
    label:{show:false},
    data:[{value:data.value.realUsers||0,name:'Real',itemStyle:{color:'#FFE600'}},
          {value:data.value.robotUsers||0,name:'Robot',itemStyle:{color:'#B1008C'}}] }]
}))

// 用户技能 & 爱好标签
const canSkillTags = computed(() => {
  return (data.value.canSkillDistribution || []).slice(0, 6).map(t => ({
    name: t.tagName, count: t.total || 0,
  }))
})
const wantSkillTags = computed(() => {
  return (data.value.wantSkillDistribution || []).slice(0, 6).map(t => ({
    name: t.tagName, count: t.total || 0,
  }))
})
const hobbyTags = computed(() => {
  return (data.value.hobbyDistribution || []).slice(0, 6).map(t => ({
    name: t.tagName, count: t.total || 0,
  }))
})

// ECharts 饼图选项
const canSkillOption = computed(() => pieChartOption(canSkillTags.value, '#0040E0'))
const wantSkillOption = computed(() => pieChartOption(wantSkillTags.value, '#B1008C'))
const hobbyOption = computed(() => pieChartOption(hobbyTags.value, '#4ADE80'))

function pieChartOption(tags, baseColor) {
  if (!tags.length) return {}
  const total = tags.reduce((s, t) => s + t.count, 0) || 1
  const colorStops = [baseColor, baseColor + '99', baseColor + '66', baseColor + '44', baseColor + '22', baseColor + '11']
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['50%', '80%'], label: { show: false },
      data: tags.map((t, i) => ({ value: t.count, name: t.name, itemStyle: { color: colorStops[i % 6] } })),
    }],
  }
}

const securityOption = computed(() => ({
  tooltip:{trigger:'item'}, series:[{ type:'pie', radius:['50%','80%'],
    label:{show:false},
    data:[{value:data.value.normalUsers||0,name:'Normal',itemStyle:{color:'#4ADE80'}},
          {value:data.value.frozenUsers||0,name:'Frozen',itemStyle:{color:'#BA1A1A'}}] }]
}))

const dailyOption = computed(() => {
  const d = data.value.dailyNewUsers || []
  return {
    tooltip:{trigger:'axis'}, grid:{left:40,right:10,top:10,bottom:20},
    xAxis:{type:'category',data:d.map(i=>i.date?.substring(5)),axisLine:{lineStyle:{color:'#000'}}},
    yAxis:{type:'value',minInterval:1,axisLine:{lineStyle:{color:'#000'}}},
    series:[
      {type:'line',data:d.map(i=>i.total),lineStyle:{color:'#FFE600',width:3},itemStyle:{color:'#FFE600'},symbol:'circle',symbolSize:6},
      {type:'line',data:d.map(i=>i.realCount),lineStyle:{color:'#0040E0',width:2},itemStyle:{color:'#0040E0'},symbol:'diamond',symbolSize:4},
      {type:'line',data:d.map(i=>i.robotCount),lineStyle:{color:'#B1008C',width:2,type:'dashed'},itemStyle:{color:'#B1008C'},symbol:'triangle',symbolSize:4},
    ],
  }
})

onMounted(async () => {
  try {
    await loadChinaMap()
    const r = await getDashboard()
    data.value = r.data || {}
  } catch { /* handled by interceptor */ }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 24px; }

/* ===== 统计卡片 ===== */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { padding: 24px; }
.stat-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.stat-icon {
  padding: 8px; border: 2px solid #000; font-size: 20px !important;
}
.stat-icon.bg-yellow { background: var(--yellow); }
.stat-icon.bg-blue { background: var(--blue); color: #fff; }
.stat-icon.bg-magenta { background: var(--magenta); color: #fff; }
.stat-badge {
  padding: 2px 8px; border: 2px solid #000; font-size: 10px; font-weight: 900; font-style: italic;
}
.stat-badge.up { background: var(--green); }
.stat-badge.stable { background: #E8E8E8; }
.stat-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; opacity: 0.5; }
.stat-value { font-size: 44px; font-weight: 900; margin-top: 2px; line-height: 1; font-family: var(--font-headline); }

.stat-export {
  background: var(--yellow); display: flex; flex-direction: column;
  align-items: center; justify-content: center; cursor: pointer;
  border: var(--border-thick); box-shadow: 8px 8px 0 #000;
  transition: transform 0.1s, box-shadow 0.1s;
}
.stat-export:active { transform: translate(4px,4px); box-shadow: 4px 4px 0 #000; }
.export-title { font-size: 22px; font-weight: 900; text-transform: uppercase; }
.export-desc { font-size: 12px; font-weight: 700; opacity: 0.7; margin-top: 2px; }

/* ===== 中排 ===== */
.mid-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
.map-card { display: flex; flex-direction: column; overflow: hidden; }
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: var(--border-thick); background: var(--surface);
}
.card-header h3 { font-family: var(--font-headline); font-size: 22px; font-weight: 900; text-transform: uppercase; font-style: italic; }
.card-header p { font-size: 11px; font-weight: 700; opacity: 0.5; text-transform: uppercase; margin-top: 2px; }
.live-badge {
  background: var(--red); color: #fff; padding: 4px 14px; border: var(--border-thick);
  font-weight: 900; font-style: italic; animation: mapPulse 2s infinite;
}
@keyframes mapPulse { 0%,100%{opacity:1} 50%{opacity:0.5} }

/* 地图画布 — 与原型一致的静态地图 + CSS 标记 */
.map-area { flex: 1; min-height: 450px; background: #f1f5f9; display: flex; align-items: center; justify-content: center; padding: 16px; }
.map-canvas {
  width: 100%; height: 100%; border: var(--border-thick); background: #fff;
  position: relative; overflow: hidden;
}
.map-loading {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #999; font-weight: 700;
  gap: 8px; font-size: 14px;
}
.spin-icon { font-size: 32px !important; animation: spin 1.5s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }
.map-empty {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #999; font-weight: 700;
  gap: 8px; font-size: 13px; z-index: 5;
}

/* CSS 叠加标记层 — 与原型 Brutalism 地图标记完全一致 */
.map-markers { position: absolute; inset: 0; pointer-events: none; z-index: 10; }
.map-marker {
  position: absolute; transform: translate(-50%, -50%); border: 3px solid #1b1b1b;
  pointer-events: auto; cursor: pointer; transition: transform 0.15s;
}
.map-marker:hover { transform: translate(-50%, -50%) scale(1.3); z-index: 20; }
/* 颜色按人数分档：黄色=人多  蓝色=中等  品红=人少 */
.marker-lg { width: 24px; height: 24px; background: var(--yellow); }
.marker-md { width: 18px; height: 18px; background: var(--blue); }
.marker-sm { width: 12px; height: 12px; background: var(--magenta); }
/* 脉冲动画 — 前3大城市 */
.marker-ping::before {
  content: '';
  position: absolute; inset: -4px;
  border: 3px solid rgba(27,27,27,0.3);
  animation: pingOuter 2s cubic-bezier(0, 0, 0.2, 1) infinite;
}
@keyframes pingOuter {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(2.5); opacity: 0; }
}
/* 标记上的城市名 */
.marker-label {
  position: absolute; left: 50%; transform: translateX(-50%); top: 100%;
  margin-top: 4px; font-size: 10px; font-weight: 700; text-transform: uppercase;
  white-space: nowrap; color: #1b1b1b; background: #fff; padding: 1px 4px;
  border: 2px solid #1b1b1b;
}

/* 城市柱状图 — 与原型一致 */
.city-card { display: flex; flex-direction: column; }
.city-bars { padding: 16px 20px; display: flex; flex-direction: column; gap: 12px; flex: 1; }
.city-bar-group { display: flex; flex-direction: column; gap: 4px; }
.city-bar-label { display: flex; justify-content: space-between; font-size: 11px; font-weight: 700; text-transform: uppercase; }
.city-bar-nums { font-family: var(--font-mono); font-size: 10px; }
.city-bar-track { height: 20px; display: flex; border: var(--border-thick); background: #fff; }
.city-bar-real { background: var(--yellow); border-right: 2px solid #000; transition: width 0.5s; }
.city-bar-robot { background: var(--magenta); transition: width 0.5s; }
.city-legend { display: flex; justify-content: center; gap: 24px; padding-top: 12px; border-top: var(--border-thick); margin-top: auto; }
.city-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #999; font-weight: 700;
  gap: 8px; font-size: 13px;
}
.legend-dot { display: inline-block; width: 12px; height: 12px; border: 2px solid #000; margin-right: 4px; vertical-align: middle; }
.legend-dot.bg-yellow { background: var(--yellow); }
.legend-dot.bg-magenta { background: var(--magenta); }
.legend-dot.bg-red { background: var(--red); }

/* ===== 底排 ===== */
.bottom-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.pie-card { padding: 20px; display: flex; flex-direction: column; align-items: center; }
.pie-title { font-family: var(--font-headline); font-size: 18px; font-weight: 900; text-transform: uppercase; font-style: italic; margin-bottom: 8px; }
.pie-legend-bar {
  display: flex; justify-content: space-between; width: 100%; margin-top: 8px;
  background: #000; color: #fff; padding: 6px 10px; font-size: 10px; font-weight: 700; text-transform: uppercase;
}
.tag-list { width: 100%; margin-top: 8px; display: flex; flex-direction: column; gap: 4px; }
.tag-item { font-size: 10px; font-weight: 700; text-transform: uppercase; display: flex; align-items: center; gap: 6px; }
.tag-dot { width: 10px; height: 10px; border: 1px solid #000; flex-shrink: 0; }
.tag-count { margin-left: auto; font-family: var(--font-mono); font-size: 11px; opacity: 0.6; }
.tag-empty { font-size: 11px; opacity: 0.4; font-weight: 700; }
.security-cards { display: flex; gap: 8px; width: 100%; margin-top: 8px; }
.sec-card {
  flex: 1; border: var(--border-thick); padding: 8px; text-align: center;
}
.sec-card.bg-yellow { background: var(--yellow); }
.sec-card.bg-red { background: var(--red); color: #fff; }
.sec-card span { display: block; font-size: 10px; font-weight: 700; text-transform: uppercase; }
.sec-card strong { font-size: 18px; font-weight: 900; font-style: italic; }

@media (max-width: 1400px) { .stats-grid{grid-template-columns:repeat(2,1fr)} .bottom-grid{grid-template-columns:repeat(2,1fr)} .mid-grid{grid-template-columns:1fr} }
@media (max-width: 1024px) { .bottom-grid{grid-template-columns:1fr} }
@media (max-width: 768px) { .stats-grid{grid-template-columns:1fr} .bottom-grid{grid-template-columns:1fr} .stat-value{font-size:36px} }
</style>
