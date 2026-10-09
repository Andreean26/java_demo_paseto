'use strict';

const runBtn = document.querySelector('#runBtn');
const runBtnText = document.querySelector('#runBtnText');
const quickBtn = document.querySelector('#quickBtn');
const roundsSelect = document.querySelector('#roundsSelect');
const presetSelect = document.querySelector('#presetSelect');
const rawSizeLabel = document.querySelector('#rawSizeLabel');
const progressWrap = document.querySelector('#progressWrap');
const progressBar = document.querySelector('#progressBar');
const envNode = document.querySelector('#envNode');
const envArch = document.querySelector('#envArch');

// KPI elements
const kpiJwtRoundtrip = document.querySelector('#kpiJwtRoundtrip');
const kpiJwtEddsaRoundtrip = document.querySelector('#kpiJwtEddsaRoundtrip');
const kpiPasetoLocRoundtrip = document.querySelector('#kpiPasetoLocRoundtrip');
const kpiPasetoPubRoundtrip = document.querySelector('#kpiPasetoPubRoundtrip');
// Chart containers
const chartGenerateTime = document.querySelector('#chartGenerateTime');
const chartVerifyTime = document.querySelector('#chartVerifyTime');
const trendBarsGrid = document.querySelector('#trendBarsGrid');
const trendTitle = document.querySelector('#trendTitle');
const trendSubtitle = document.querySelector('#trendSubtitle');
const chartSize = document.querySelector('#chartSize');

// Token inspector
const tokenTabs = document.querySelector('#tokenTabs');
const tokenPreviewCode = document.querySelector('#tokenPreviewCode');

let lastBenchmarkData = null;
let activeTokenTab = 'jwt';

function formatNumber(num) {
  return new Intl.NumberFormat('id-ID').format(Math.round(num));
}

function renderBarChart(container, items, { unit = '' } = {}) {
  container.innerHTML = '';
  if (!items || !items.length) return;

  const maxVal = Math.max(...items.map((i) => (i.barVal !== undefined ? i.barVal : i.value)), 1);

  for (const item of items) {
    const row = document.createElement('div');
    row.className = 'bar-row';

    const labelWrap = document.createElement('div');
    labelWrap.className = 'bar-label-wrap';

    const name = document.createElement('span');
    name.className = 'bar-name';
    name.textContent = item.label;

    const val = document.createElement('span');
    val.className = 'bar-value';
    val.textContent = item.displayValue || `${formatNumber(item.value)} ${unit}`.trim();

    labelWrap.append(name, val);

    const track = document.createElement('div');
    track.className = 'bar-track';

    const fill = document.createElement('div');
    fill.className = `bar-fill ${item.colorClass || 'cyan'}`;
    const metricVal = item.barVal !== undefined ? item.barVal : item.value;
    const percentage = Math.max(4, Math.min(100, (metricVal / maxVal) * 100));
    fill.style.width = '0%';
    setTimeout(() => {
      fill.style.width = `${percentage}%`;
    }, 40);

    track.append(fill);
    row.append(labelWrap, track);
    container.append(row);
  }
}

// Visualisasi Grafik Multi-Round (Trend Konsistensi 10 Putaran)
function renderTrendBars(container, roundsHistory) {
  container.innerHTML = '';
  if (!roundsHistory || !roundsHistory.length) return;

  // Cari ops/sec tertinggi di seluruh putaran untuk skala tinggi 100%
  let maxOps = 1;
  for (const r of roundsHistory) {
    maxOps = Math.max(
      maxOps,
      r.jwtHs?.roundtripOpsSec || 0,
      r.jwtEddsa?.roundtripOpsSec || 0,
      r.pasetoLoc?.roundtripOpsSec || 0,
      r.pasetoPub?.roundtripOpsSec || 0
    );
  }
  for (const roundData of roundsHistory) {
    const col = document.createElement('div');
    col.className = 'trend-col';

    const barsGroup = document.createElement('div');
    barsGroup.className = 'trend-bars-group';

    // 1. JWT Bar
    const jwtOps = roundData.jwtHs.roundtripOpsSec || 0;
    const jwtHeight = Math.max(4, Math.round((jwtOps / maxOps) * 100));
    const jwtBar = document.createElement('div');
    jwtBar.className = 'trend-bar jwt';
    jwtBar.style.height = `${jwtHeight}%`;
    jwtBar.title = `Putaran ${roundData.round} - JWT HS256: ${formatNumber(jwtOps)} ops/s (${roundData.jwtHs.avgLatencyMs || roundData.jwtHs.avgLatencyUs} ms)`;

    // 2. JWT EdDSA Bar (Ed25519)
    const eddsaOps = roundData.jwtEddsa?.roundtripOpsSec || 0;
    const eddsaHeight = Math.max(4, Math.round((eddsaOps / maxOps) * 100));
    const eddsaBar = document.createElement('div');
    eddsaBar.className = 'trend-bar jwt-eddsa';
    eddsaBar.style.height = `${eddsaHeight}%`;
    eddsaBar.title = `Putaran ${roundData.round} - JWT EdDSA: ${formatNumber(eddsaOps)} ops/s (${roundData.jwtEddsa?.avgLatencyMs || roundData.jwtEddsa?.avgLatencyUs || 0} ms)`;
    const locOps = roundData.pasetoLoc.roundtripOpsSec || 0;
    const locHeight = Math.max(4, Math.round((locOps / maxOps) * 100));
    const locBar = document.createElement('div');
    locBar.className = 'trend-bar paseto-loc';
    locBar.style.height = `${locHeight}%`;
    locBar.title = `Putaran ${roundData.round} - PASETO Local: ${formatNumber(locOps)} ops/s (${roundData.pasetoLoc.avgLatencyMs || roundData.pasetoLoc.avgLatencyUs} ms)`;

    // 3. PASETO v4.public Bar
    const pubOps = roundData.pasetoPub.roundtripOpsSec || 0;
    const pubHeight = Math.max(4, Math.round((pubOps / maxOps) * 100));
    const pubBar = document.createElement('div');
    pubBar.className = 'trend-bar paseto-pub';
    pubBar.style.height = `${pubHeight}%`;
    pubBar.title = `Putaran ${roundData.round} - PASETO Public: ${formatNumber(pubOps)} ops/s (${roundData.pasetoPub.avgLatencyMs || roundData.pasetoPub.avgLatencyUs} ms)`;

    barsGroup.append(jwtBar, eddsaBar, locBar, pubBar);
    const label = document.createElement('span');
    label.className = 'trend-col-label';
    label.textContent = `R${roundData.round}`;

    col.append(barsGroup, label);
    container.append(col);
  }
}

function updateKpiCards(results) {
  const { jwtHs, jwtEddsa, pasetoLoc, pasetoPub } = results;

  if (kpiJwtRoundtrip && jwtHs) {
    kpiJwtRoundtrip.textContent = `${formatNumber(jwtHs.performance.roundtrip.opsSec)} ops/s`;
    const sub = document.querySelector('#kpiJwtSub');
    if (sub) sub.textContent = `Symmetric MAC \u2022 Latensi: ${jwtHs.performance.roundtrip.avgLatencyMs || jwtHs.performance.roundtrip.avgLatencyUs} ms`;
  }
  if (kpiJwtEddsaRoundtrip && jwtEddsa) {
    kpiJwtEddsaRoundtrip.textContent = `${formatNumber(jwtEddsa.performance.roundtrip.opsSec)} ops/s`;
    const sub = document.querySelector('#kpiJwtEddsaSub');
    if (sub) sub.textContent = `Asymmetric Ed25519 \u2022 Latensi: ${jwtEddsa.performance.roundtrip.avgLatencyMs || jwtEddsa.performance.roundtrip.avgLatencyUs} ms`;
  }
  if (kpiPasetoLocRoundtrip && pasetoLoc) {
    kpiPasetoLocRoundtrip.textContent = `${formatNumber(pasetoLoc.performance.roundtrip.opsSec)} ops/s`;
    const sub = document.querySelector('#kpiPasetoLocSub');
    if (sub) sub.textContent = `Symmetric AEAD \u2022 Latensi: ${pasetoLoc.performance.roundtrip.avgLatencyMs || pasetoLoc.performance.roundtrip.avgLatencyUs} ms`;
  }
  if (kpiPasetoPubRoundtrip && pasetoPub) {
    kpiPasetoPubRoundtrip.textContent = `${formatNumber(pasetoPub.performance.roundtrip.opsSec)} ops/s`;
    const sub = document.querySelector('#kpiPasetoPubSub');
    if (sub) sub.textContent = `Asymmetric Ed25519 \u2022 Latensi: ${pasetoPub.performance.roundtrip.avgLatencyMs || pasetoPub.performance.roundtrip.avgLatencyUs} ms`;
  }
}

function updateCharts(data) {
  const { results, roundsHistory, benchmarkMeta } = data;
  const { jwtHs, jwtEddsa, pasetoLoc, pasetoPub } = results;

  // 1A. Grafik Waktu Pembuatan (Generate Time — Signing / Enkripsi)
  const jwtSignMs = (jwtHs.performance.sign.stats?.mean || 0).toFixed(3);
  const jwtEddsaSignMs = (jwtEddsa?.performance.sign.stats?.mean || 0).toFixed(3);
  const locEncMs = (pasetoLoc.performance.encrypt.stats?.mean || 0).toFixed(3);
  const pubSignMs = (pasetoPub.performance.sign.stats?.mean || 0).toFixed(3);

  renderBarChart(
    chartGenerateTime,
    [
      {
        label: 'JWT (HS256) — Signing HMAC',
        value: Number(jwtSignMs),
        barVal: jwtHs.performance.sign.opsSec,
        displayValue: `${jwtSignMs} ms (${formatNumber(jwtHs.performance.sign.opsSec)} ops/s)`,
        colorClass: 'cyan'
      },
      {
        label: 'JWT (EdDSA / Ed25519) — Asymmetric Sign',
        value: Number(jwtEddsaSignMs),
        barVal: jwtEddsa?.performance.sign.opsSec || 0,
        displayValue: `${jwtEddsaSignMs} ms (${formatNumber(jwtEddsa?.performance.sign.opsSec || 0)} ops/s)`,
        colorClass: 'indigo'
      },
      {
        label: 'PASETO (v4.local) — Enkripsi XChaCha20 + BLAKE2b',
        value: Number(locEncMs),
        barVal: pasetoLoc.performance.encrypt.opsSec,
        displayValue: `${locEncMs} ms (${formatNumber(pasetoLoc.performance.encrypt.opsSec)} ops/s)`,
        colorClass: 'green'
      },
      {
        label: 'PASETO (v4.public) — Digital Signature Ed25519',
        value: Number(pubSignMs),
        barVal: pasetoPub.performance.sign.opsSec,
        displayValue: `${pubSignMs} ms (${formatNumber(pasetoPub.performance.sign.opsSec)} ops/s)`,
        colorClass: 'yellow'
      }
    ],
    { unit: 'ms' }
  );

  // 1B. Grafik Waktu Verifikasi (Verify Time — Validasi / Dekripsi)
  const jwtVerifyMs = (jwtHs.performance.verify.stats?.mean || 0).toFixed(3);
  const jwtEddsaVerifyMs = (jwtEddsa?.performance.verify.stats?.mean || 0).toFixed(3);
  const locDecMs = (pasetoLoc.performance.decrypt.stats?.mean || 0).toFixed(3);
  const pubVerifyMs = (pasetoPub.performance.verify.stats?.mean || 0).toFixed(3);

  renderBarChart(
    chartVerifyTime,
    [
      {
        label: 'JWT (HS256) — Validasi Signature HMAC',
        value: Number(jwtVerifyMs),
        barVal: jwtHs.performance.verify.opsSec,
        displayValue: `${jwtVerifyMs} ms (${formatNumber(jwtHs.performance.verify.opsSec)} ops/s)`,
        colorClass: 'cyan'
      },
      {
        label: 'JWT (EdDSA / Ed25519) — Verifikasi Public Key',
        value: Number(jwtEddsaVerifyMs),
        barVal: jwtEddsa?.performance.verify.opsSec || 0,
        displayValue: `${jwtEddsaVerifyMs} ms (${formatNumber(jwtEddsa?.performance.verify.opsSec || 0)} ops/s)`,
        colorClass: 'indigo'
      },
      {
        label: 'PASETO (v4.local) — Dekripsi & Tag BLAKE2b',
        value: Number(locDecMs),
        barVal: pasetoLoc.performance.decrypt.opsSec,
        displayValue: `${locDecMs} ms (${formatNumber(pasetoLoc.performance.decrypt.opsSec)} ops/s)`,
        colorClass: 'green'
      },
      {
        label: 'PASETO (v4.public) — Verifikasi Kunci Publik Ed25519',
        value: Number(pubVerifyMs),
        barVal: pasetoPub.performance.verify.opsSec,
        displayValue: `${pubVerifyMs} ms (${formatNumber(pasetoPub.performance.verify.opsSec)} ops/s)`,
        colorClass: 'yellow'
      }
    ],
    { unit: 'ms' }
  );
  // 2. Grafik Konsistensi 10 Putaran (Trend Bar Columns)
  if (trendTitle && benchmarkMeta) {
    trendTitle.textContent = `2. Grafik Konsistensi ${benchmarkMeta.rounds} Putaran Pengujian`;
  }
  if (trendSubtitle && benchmarkMeta) {
    trendSubtitle.textContent = `Membandingkan kestabilan throughput masing-masing token dari Putaran 1 sampai Putaran ${benchmarkMeta.rounds} (Arahkan kursor pada bar untuk melihat detail angka).`;
  }
  renderTrendBars(trendBarsGrid, roundsHistory);

  // 3. Grafik Ukuran Token & Overhead Bytes
  renderBarChart(
    chartSize,
    [
      {
        label: 'Raw JSON Payload Asli (Tanpa Header / Signature)',
        value: benchmarkMeta.rawPayloadBytes,
        colorClass: 'muted-bar'
      },
      {
        label: `JWT (HS256) — Overhead +${jwtHs.overheadBytes} B (+${jwtHs.overheadPercentage}%)`,
        value: jwtHs.byteSize,
        colorClass: 'cyan'
      },
      {
        label: `JWT (EdDSA / Ed25519) — Overhead +${jwtEddsa?.overheadBytes || 0} B (+${jwtEddsa?.overheadPercentage || 0}%)`,
        value: jwtEddsa?.byteSize || 0,
        colorClass: 'indigo'
      },
      {
        label: `PASETO (v4.local) — Overhead +${pasetoLoc.overheadBytes} B (+${pasetoLoc.overheadPercentage}%)`,
        value: pasetoLoc.byteSize,
        colorClass: 'green'
      },
      {
        label: `PASETO (v4.public) — Overhead +${pasetoPub.overheadBytes} B (+${pasetoPub.overheadPercentage}%)`,
        value: pasetoPub.byteSize,
        colorClass: 'yellow'
      }
    ],
    { unit: 'Bytes' }
  );

  // Sinkronisasi angka tabel Matriks Evaluasi Menyeluruh dengan hasil test riil
  const setEl = (id, txt) => {
    const el = document.getElementById(id);
    if (el) el.textContent = txt;
  };

  if (jwtHs && jwtEddsa) {
    setEl('matrixJwtHsOps', `${formatNumber(jwtHs.performance.roundtrip.opsSec)} ops/s`);
    setEl('matrixJwtHsLat', `${jwtHs.performance.roundtrip.avgLatencyMs} ms`);
    setEl('matrixJwtHsSize', `${jwtHs.byteSize} Bytes`);
    setEl('matrixJwtHsOverhead', `+${jwtHs.overheadBytes} B (+${jwtHs.overheadPercentage}% dari raw JSON)`);

    setEl('matrixJwtEddsaOps', `${formatNumber(jwtEddsa.performance.roundtrip.opsSec)} ops/s`);
    setEl('matrixJwtEddsaLat', `${jwtEddsa.performance.roundtrip.avgLatencyMs} ms`);
    setEl('matrixJwtEddsaSize', `${jwtEddsa.byteSize} Bytes`);
    setEl('matrixJwtEddsaOverhead', `+${jwtEddsa.overheadBytes} B (+${jwtEddsa.overheadPercentage}% dari raw JSON)`);

    setEl('matrixJwtOverheadBadge', `Overhead +${jwtHs.overheadPercentage}% s/d +${jwtEddsa.overheadPercentage}% dari Raw JSON`);
  }

  if (pasetoLoc && pasetoPub) {
    setEl('matrixPasetoLocOps', `${formatNumber(pasetoLoc.performance.roundtrip.opsSec)} ops/s`);
    setEl('matrixPasetoLocLat', `${pasetoLoc.performance.roundtrip.avgLatencyMs} ms`);
    setEl('matrixPasetoLocSize', `${pasetoLoc.byteSize} Bytes`);
    setEl('matrixPasetoLocOverhead', `+${pasetoLoc.overheadBytes} B (+${pasetoLoc.overheadPercentage}% dari raw JSON)`);

    setEl('matrixPasetoPubOps', `${formatNumber(pasetoPub.performance.roundtrip.opsSec)} ops/s`);
    setEl('matrixPasetoPubLat', `${pasetoPub.performance.roundtrip.avgLatencyMs} ms`);
    setEl('matrixPasetoPubSize', `${pasetoPub.byteSize} Bytes`);
    setEl('matrixPasetoPubOverhead', `+${pasetoPub.overheadBytes} B (+${pasetoPub.overheadPercentage}% dari raw JSON)`);

    setEl('matrixPasetoOverheadBadge', `Overhead +${pasetoLoc.overheadPercentage}% s/d +${pasetoPub.overheadPercentage}% dari Raw JSON`);

    if (jwtEddsa) {
      const diff = jwtEddsa.byteSize - pasetoPub.byteSize;
      if (diff > 0) {
        setEl('matrixPasetoDiffNote', `Pada mode Ed25519, PASETO v4.public justru ${diff} Byte LEBIH KECIL dari JWT EdDSA (${pasetoPub.byteSize} B vs ${jwtEddsa.byteSize} B)!`);
      } else {
        setEl('matrixPasetoDiffNote', `Pada mode Ed25519, ukuran PASETO v4.public setara dengan JWT EdDSA (${pasetoPub.byteSize} B vs ${jwtEddsa.byteSize} B).`);
      }
    }
  }
}

function updateTokenInspector() {
  if (!lastBenchmarkData || !lastBenchmarkData.results) return;
  const { results } = lastBenchmarkData;

  let currentRawToken = '';
  if (activeTokenTab === 'jwt') {
    currentRawToken = results.jwtHs.token;
  } else if (activeTokenTab === 'jwtEddsa') {
    currentRawToken = results.jwtEddsa?.token;
  } else if (activeTokenTab === 'pasetoLoc') {
    currentRawToken = results.pasetoLoc.token;
  } else if (activeTokenTab === 'pasetoPub') {
    currentRawToken = results.pasetoPub.token;
  }
  tokenPreviewCode.textContent = currentRawToken || '-';
}

async function runBenchmark(customRounds = null) {
  const rounds = customRounds || Number(roundsSelect ? roundsSelect.value : 10);
  const preset = presetSelect ? presetSelect.value : 'standard';

  runBtn.disabled = true;
  if (quickBtn) quickBtn.disabled = true;
  runBtnText.textContent = 'Menguji Benchmark...';
  progressWrap.hidden = false;
  progressBar.style.width = '30%';

  try {
    const response = await fetch('/api/benchmark', {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ rounds, preset })
    });

    progressBar.style.width = '80%';
    const data = await response.json();
    progressBar.style.width = '100%';

    if (!data.ok) {
      throw new Error(data.error || 'Benchmark gagal.');
    }

    lastBenchmarkData = data;

    // Metadata environment
    if (data.benchmarkMeta) {
      rawSizeLabel.textContent = `${data.benchmarkMeta.rawPayloadBytes} Bytes`;
      if (data.benchmarkMeta.environment) {
        renderEnvironmentBadge(data.benchmarkMeta.environment);
      }
    }
    // Render 3 Visual Graphs & KPIs
    updateKpiCards(data.results);
    updateCharts(data);
    updateTokenInspector();
  } catch (error) {
    alert(`Gagal menjalankan benchmark: ${error.message}`);
  } finally {
    setTimeout(() => {
      progressWrap.hidden = true;
      progressBar.style.width = '0%';
      runBtn.disabled = false;
      if (quickBtn) quickBtn.disabled = false;
      runBtnText.textContent = 'Jalankan Benchmark';
    }, 400);
  }
}

// Event Listeners
tokenTabs.addEventListener('click', (event) => {
  const btn = event.target.closest('.tab-btn');
  if (!btn) return;
  tokenTabs.querySelectorAll('.tab-btn').forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  activeTokenTab = btn.dataset.target;
  updateTokenInspector();
});

runBtn.addEventListener('click', () => runBenchmark());

// Render environment badge immediately on page load without waiting for benchmark trigger
function renderEnvironmentBadge(env) {
  if (!env) return;
  const jVersion = env.javaVersion || '17';
  const sVersion = env.springBootVersion ? ` / Spring Boot ${env.springBootVersion}` : '';
  if (envNode) envNode.textContent = `Java ${jVersion}${sVersion}`;
  const cpuName = env.cpuModel || env.arch;
  const osText = env.platform ? ` \u2022 ${env.platform}` : '';
  if (envArch) envArch.textContent = `${env.cpus} CPU Cores \u2022 ${cpuName}${osText}`;
}

async function loadEnvironmentImmediately() {
  try {
    const res = await fetch('/api/benchmark/environment');
    if (res.ok) {
      const env = await res.json();
      renderEnvironmentBadge(env);
    }
  } catch (e) {}
}

loadEnvironmentImmediately();

/* ============================================================================
   COACH MARK / GUIDED TOUR BENCHMARK
   ============================================================================ */
const benchmarkTourSteps = [
  {
    element: '.benchmark-controls-panel',
    title: 'Langkah 1: Konfigurasi Benchmark Multi-Putaran',
    description: 'Atur jumlah putaran sampel statistik (10x hingga 30x) dan pilih preset ukuran payload JSON (dari 50 Bytes hingga 5 KB enterprise).'
  },
  {
    element: '#kpiGrid',
    title: 'Langkah 2: Throughput Riil (Ops/Detik)',
    description: 'Kartu metrik utama menampilkan rata-rata operasi per detik: <strong>JWT HS256</strong> (HMAC), <strong>JWT EdDSA</strong> (Ed25519), <strong>PASETO v4.local</strong> (XChaCha20 + BLAKE2b-MAC), dan <strong>PASETO v4.public</strong> (Ed25519).'
  },
  {
    element: '.charts-container',
    title: 'Langkah 3: Analisis Waktu (Generate vs Verify)',
    description: 'Grafik batang membandingkan waktu proses enkripsi/signing saat pembuatan token dibanding waktu verifikasi token di sisi server API.'
  },
  {
    element: '.token-inspector-panel',
    title: 'Langkah 4: Inspeksi String & Byte Token',
    description: 'Bandingkan output string token yang dihasilkan secara langsung serta ukuran byte overhead di header HTTP.'
  },
  {
    element: '.matrix-panel',
    title: 'Langkah 5: Matriks Evaluasi Arsitektural',
    description: 'Pelajari tabel analisis parameter kriptografi mendalam: mengapa PASETO kebal dari eksploitasi <code>alg:none</code> dan key confusion dibanding JWT.'
  }
];

function startBenchmarkTour() {
  if (window.CoachMark) {
    window.CoachMark.start(benchmarkTourSteps, {
      onComplete: () => {
        localStorage.setItem('tour_seen_benchmark', 'true');
      }
    });
  }
}

const startTourBtn = document.querySelector('#startTourBtn');
if (startTourBtn) {
  startTourBtn.addEventListener('click', startBenchmarkTour);
}

if (!localStorage.getItem('tour_seen_benchmark')) {
  setTimeout(() => {
    startBenchmarkTour();
  }, 1200);
}
