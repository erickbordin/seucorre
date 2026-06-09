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
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`);

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
const TYPE_METADATA = {
  INTERVALADO: { key: 'intervals', label: 'Intervalado' },
  FARTLAKE: { key: 'tempo', label: 'Fartlek' },
  LONGO: { key: 'long_run', label: 'Longão' },
  REGENERATIVO: { key: 'recovery', label: 'Recuperação' },
};

const normalizeTipo = (tipo) => {
  return TYPE_METADATA[tipo]?.key || 'easy_run';
};

const normalizeWorkoutStatus = (session) => {
  const registroStatus = String(session?.registro?.status || '').toLowerCase();
  if (registroStatus) {
    if (registroStatus === 'parcial') return 'parcial';
    if (registroStatus === 'concluido') return 'concluido';
    if (registroStatus === 'perdido') return 'perdido';
  }
  if (session?.executada) return 'concluido';
  if (session?.atrasada) return 'perdido';
  return 'pendente';
};

export function normalizeSession(session) {
  if (!session) return null;
  const parsedDate = session.dataPrevista ? new Date(`${session.dataPrevista}T00:00:00`) : null;
  const readableTipo = TYPE_METADATA[session.tipo]?.label || (session.tipo ? session.tipo.replaceAll('_', ' ') : 'Treino');
  return {
    ...session,
    semana: session.numeroSemana,
    dia_semana: parsedDate ? parsedDate.toLocaleDateString('pt-BR', { weekday: 'short' }).slice(0, 3).toLowerCase().replace('.', '') : null,
    dataLabel: parsedDate ? parsedDate.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' }) : null,
    tipo: normalizeTipo(session.tipo),
    tipoOriginal: session.tipo,
    tipoLabel: readableTipo,
    titulo: readableTipo,
    duracao_min: session.duracaoMinutos,
    distancia_km: Number(session.distanciaKm || 0),
    pace_alvo_min: session.paceAlvo ? `${session.paceAlvo} min/km` : null,
    pace_alvo_max: null,
    zona_fc: session.zonaFcAlvo,
    status: normalizeWorkoutStatus(session),
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
  const normalizedToday = new Date(today.toDateString());
  const current = sessoes.find((s) => s?.dataPrevista && new Date(`${s.dataPrevista}T00:00:00`) >= normalizedToday);
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

export function normalizeCheckin(item) {
  if (!item) return null;
  return {
    ...item,
    nivelRiscoLabel: String(item.nivelRisco || '').replaceAll('_', ' '),
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
  checkins: {
    historico: () => request('/api/checkins/historico').then((items) => (items || []).map(normalizeCheckin)),
    enviar: (payload) => request('/api/checkins', { method: 'POST', body: payload }).then(normalizeCheckin),
  },
  dispositivos: {
    listar: () => request('/api/dispositivos'),
    conectar: (payload) => request('/api/dispositivos', { method: 'POST', body: payload }),
    sincronizar: (plataforma) => request(`/api/dispositivos/${plataforma}/sincronizar`, { method: 'POST' }),
  },

};
