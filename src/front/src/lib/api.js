const API_BASE_URL = import.meta.env.VITE_API_URL || '';
const ACCESS_TOKEN_KEY = 'seucorre_access_token';
const REFRESH_TOKEN_KEY = 'seucorre_refresh_token';

export const tokenStore = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: ({ token, refreshToken }) => {
    if (token) localStorage.setItem(ACCESS_TOKEN_KEY, token);
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

async function parseResponse(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function request(path, options = {}, retry = true) {
  const headers = new Headers(options.headers || {});
  const hasBody = options.body !== undefined;
  if (hasBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const token = tokenStore.getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: hasBody && typeof options.body !== 'string' ? JSON.stringify(options.body) : options.body,
  });

  if (response.status === 401 && retry && tokenStore.getRefreshToken()) {
    const refreshed = await auth.refresh().catch(() => null);
    if (refreshed) return request(path, options, false);
  }

  const data = await parseResponse(response);
  if (!response.ok) {
    const message = data?.mensagem || data?.message || data?.erro || `Erro HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

const normalizeStatus = (status) => String(status || '').toLowerCase();
const normalizeTipo = (tipo) => {
  const map = {
    INTERVALADO: 'intervals',
    FARTLAKE: 'tempo',
    LONGO: 'long_run',
    REGENERATIVO: 'recovery',
  };
  return map[tipo] || 'easy_run';
};

export function normalizeSession(session) {
  if (!session) return null;
  return {
    ...session,
    semana: session.numeroSemana,
    dia_semana: session.dataPrevista ? new Date(`${session.dataPrevista}T00:00:00`).toLocaleDateString('pt-BR', { weekday: 'short' }).slice(0, 3).toLowerCase().replace('.', '') : null,
    tipo: normalizeTipo(session.tipo),
    tipoOriginal: session.tipo,
    titulo: session.tipo ? session.tipo.replaceAll('_', ' ') : 'Treino',
    duracao_min: session.duracaoMinutos,
    distancia_km: Number(session.distanciaKm || 0),
    pace_alvo_min: session.paceAlvo ? `${session.paceAlvo} min/km` : null,
    pace_alvo_max: null,
    zona_fc: session.zonaFcAlvo,
    status: session.executada ? 'concluido' : session.atrasada ? 'perdido' : 'pendente',
    estrutura: [
      { fase: 'aquecimento', descricao: 'Aqueça progressivamente antes do bloco principal.', duracao: '10 min', tipo_atividade: 'trotar' },
      { fase: 'principal', descricao: session.descricao || 'Siga o treino no ritmo indicado.', duracao: `${session.duracaoMinutos || 0} min`, tipo_atividade: 'correr' },
      { fase: 'volta_calma', descricao: 'Finalize em ritmo confortável e alongue se necessário.', duracao: '5 min', tipo_atividade: 'caminhar' },
    ],
  };
}

export function normalizePlan(plan) {
  if (!plan) return null;
  const sessoes = (plan.sessoes || []).map(normalizeSession);
  const today = new Date();
  const current = sessoes.find((s) => s?.dataPrevista && new Date(`${s.dataPrevista}T00:00:00`) >= new Date(today.toDateString()));
  return {
    ...plan,
    nome: 'Plano de Corrida',
    descricao: plan.resumoIA,
    semana_atual: current?.semana || 1,
    total_semanas: plan.totalSemanas || 4,
    status: normalizeStatus(plan.status),
    sessoes,
    ia_insights: plan.resumoIA,
  };
}

export const auth = {
  login: async (email, senha) => {
    const data = await request('/auth/login', { method: 'POST', body: { email, senha } }, false);
    tokenStore.setTokens(data);
    return data;
  },
  refresh: async () => {
    const refreshToken = tokenStore.getRefreshToken();
    if (!refreshToken) return null;
    const data = await request('/auth/refresh', {
      method: 'POST',
      headers: { Authorization: `Bearer ${refreshToken}` },
    }, false);
    tokenStore.setTokens(data);
    return data;
  },
  logout: () => tokenStore.clear(),
};

export const api = {
  request,
  usuarios: {
    me: () => request('/api/usuarios/me'),
    registrar: (payload) => request('/api/usuarios/registrar', { method: 'POST', body: payload }, false),
    atualizarOnboarding: (payload) => request('/api/usuarios/me/onboarding', { method: 'PUT', body: payload }),
  },
  planos: {
    gerar: (objetivo) => request('/api/planos/gerar', { method: 'POST', body: { objetivo } }).then(normalizePlan),
    listarMeus: () => request('/api/planos/me').then((plans) => (plans || []).map(normalizePlan)),
    buscar: (id) => request(`/api/planos/${id}`).then(normalizePlan),
    pausar: (id) => request(`/api/planos/${id}/pausar`, { method: 'PATCH' }).then(normalizePlan),
    reativar: (id) => request(`/api/planos/${id}/reativar`, { method: 'PATCH' }).then(normalizePlan),
  },
  treinos: {
    listarHistorico: () => request('/api/treinos/historico').then((items) => (items || []).map(normalizeSession)),
    registrar: (treinoId, payload) => request(`/api/treinos/${treinoId}/registrar`, {
      method: 'PATCH',
      body: { treinoId, ...payload },
    }),
  },
  progresso: {
    historico: () => request('/api/progresso/historico'),
    visaoGeral: () => request('/api/progresso/visao-geral'),
  },
};
