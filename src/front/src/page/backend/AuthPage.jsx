import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronLeft, Loader2, LogIn, Mail, ShieldCheck, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import DaySelector from '@/components/onboarding/DaySelector';
import SelectionCard from '@/components/onboarding/SelectionCard';
import {
  ageFromBirthDate,
  birthDateFromAge,
  parseTrainingDays,
  serializeTrainingDays,
} from '@/lib/user';

const DISTANCE_GOALS = [
  { value: '3K', label: '3 km', description: 'Comecar leve e ganhar consistencia' },
  { value: '5K', label: '5 km', description: 'Recomendado para construir base com seguranca', recommended: true },
  { value: '10K', label: '10 km', description: 'Disponivel para quem esta muito ativo' },
];

const RUNNER_GOALS = [
  { value: 'COMPLETAR_5K', label: '5 km', description: 'Completar a distancia com boa base' },
  { value: 'COMPLETAR_10K', label: '10 km', description: 'Evoluir resistencia e ritmo' },
  { value: 'COMPLETAR_15K', label: '15 km', description: 'Subir volume sem perder controle' },
  { value: 'COMPLETAR_MEIA_MARATONA', label: '21 km', description: 'Preparar meia maratona' },
  { value: 'COMPLETAR_MARATONA', label: '42 km', description: 'Preparar maratona' },
  { value: 'MELHORAR_DISTANCIA', label: 'Melhorar marca', description: 'Baixar tempo em uma distancia' },
  { value: 'VOLTAR_A_CORRER', label: 'Voltar a correr', description: 'Retomar rotina com progressao' },
];

const LEVELS = [
  { value: 'INICIANTE', label: 'Iniciante', description: 'Corre pouco ou esta retomando do zero' },
  { value: 'INTERMEDIARIO', label: 'Intermediario', description: 'Ja treina com alguma frequencia' },
  { value: 'AVANCADO', label: 'Avancado', description: 'Treina com consistencia e usa zonas/ritmos' },
];

const RP_DISTANCES = [
  { value: '5', label: '5 km' },
  { value: '10', label: '10 km' },
  { value: '21', label: '21 km' },
  { value: '42', label: '42 km' },
];

const createEmptyForm = () => ({
  nome: '',
  email: '',
  senha: '',
  telefone: '',
  idade: '',
  alturaCm: '',
  pesoKg: '',
  jaCorre: null,
  fazAtividade: null,
  atividadeNivel: '',
  horasSonoMedia: '',
  diasSemanaTreino: ['seg', 'qua', 'sex'],
  semanasTreino: '8',
  objetivoNaoCorredor: '5K',
  objetivoCorredor: 'COMPLETAR_10K',
  nivelCorrida: '',
  distanciaRp: '5',
  paceRp: '',
  zonasPace: {
    z1: '',
    z2: '',
    z3: '',
    z4: '',
    z5: '',
  },
});

const buildFormFromUser = (user) => {
  const days = parseTrainingDays(user?.diasSemanaTreino);

  return {
    ...createEmptyForm(),
    nome: user?.nome || '',
    email: user?.email || '',
    idade: ageFromBirthDate(user?.dataNascimento),
    pesoKg: user?.pesoKg != null ? String(user.pesoKg) : '',
    alturaCm: user?.alturaCm != null ? String(user.alturaCm) : '',
    jaCorre: user?.jaCorre ?? null,
    diasSemanaTreino: days.length > 0 ? days : ['seg', 'qua', 'sex'],
    horasSonoMedia: user?.horasSonoMedia != null ? String(user.horasSonoMedia) : '',
    nivelCorrida: user?.nivelCondicionamento || '',
    objetivoCorredor: user?.objetivo || 'COMPLETAR_10K',
    objetivoNaoCorredor: user?.objetivo === 'COMPLETAR_10K' ? '10K' : user?.objetivo === 'COMPLETAR_5K' ? '5K' : '5K',
    paceRp: user?.perfilCorrida?.pace5kMinKm ? String(user.perfilCorrida.pace5kMinKm) : '',
  };
};

const toOptionalInteger = (value) => {
  if (value === '' || value == null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const toOptionalDecimal = (value) => {
  if (value === '' || value == null) return null;
  const normalized = String(value).replace(',', '.');
  const parsed = Number(normalized);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
};

const getNoRunnerGoal = (value) => {
  if (value === '10K') return 'COMPLETAR_10K';
  return 'COMPLETAR_5K';
};

const getRunnerGoal = (form) => {
  if (form.objetivoCorredor === 'MELHORAR_DISTANCIA') {
    if (form.distanciaRp === '42') return 'MELHORAR_MARATONA';
    if (form.distanciaRp === '21') return 'MELHORAR_MEIA_MARATONA';
    if (form.distanciaRp === '10') return 'MELHORAR_10K';
    return 'MELHORAR_5K';
  }

  if (form.objetivoCorredor === 'VOLTAR_A_CORRER') return 'SAUDE_GERAL';
  if (form.objetivoCorredor === 'COMPLETAR_15K') return 'COMPLETAR_10K';
  return form.objetivoCorredor;
};

const buildPerfilCorrida = (form) => {
  const pace = toOptionalDecimal(form.paceRp);
  if (!pace) return null;

  return {
    pace5kMinKm: form.distanciaRp === '5' ? pace : null,
    pace10kMinKm: form.distanciaRp === '10' ? pace : null,
    pace21kMinKm: form.distanciaRp === '21' ? pace : null,
    pace42kMinKm: form.distanciaRp === '42' ? pace : null,
    vo2Estimado: null,
    zonasFc: [],
  };
};

const buildProfilePayload = (form) => {
  const runnerLevel = form.jaCorre ? form.nivelCorrida : 'INICIANTE';
  const isBeginnerRunner = form.jaCorre && form.nivelCorrida === 'INICIANTE';
  const useNoRunnerFlow = !form.jaCorre || isBeginnerRunner;
  const nivelCondicionamento = useNoRunnerFlow
    ? form.atividadeNivel === 'MUITO_ATIVO' ? 'INTERMEDIARIO' : 'INICIANTE'
    : runnerLevel;

  return {
    telefone: form.telefone || null,
    dadosFisicos: {
      pesoKg: Number(form.pesoKg),
      alturaCm: Number(form.alturaCm),
      dataNascimento: birthDateFromAge(form.idade),
      genero: 'MASCULINO',
      fcRepouso: null,
      fcMaxima: null,
      horasSonoMedia: toOptionalInteger(form.horasSonoMedia),
      sedentario: form.atividadeNivel !== 'MUITO_ATIVO' && nivelCondicionamento === 'INICIANTE',
    },
    perfilAtleta: {
      nivelCondicionamento,
      objetivo: useNoRunnerFlow ? getNoRunnerGoal(form.objetivoNaoCorredor) : getRunnerGoal(form),
      jaCorre: Boolean(form.jaCorre),
      diasDisponiveisSemana: form.diasSemanaTreino.length,
      diasSemanaTreino: serializeTrainingDays(form.diasSemanaTreino),
    },
    perfilCorrida: useNoRunnerFlow ? null : buildPerfilCorrida(form),
    condicoesSaude: [],
    dispositivos: [],
  };
};

const GoogleButton = ({ label, onClick }) => (
  <Button
    type="button"
    variant="outline"
    onClick={onClick}
    className="h-12 w-full rounded-xl border-border bg-card text-foreground hover:bg-secondary"
  >
    <span className="mr-2 flex h-5 w-5 items-center justify-center rounded-full bg-foreground text-[11px] font-bold text-background">G</span>
    {label}
  </Button>
);

export default function AuthPage({ mode = 'register' }) {
  const navigate = useNavigate();
  const { hasCompletedOnboarding, login, reloadUser, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(createEmptyForm);
  const [profileStep, setProfileStep] = useState(0);

  const isLogin = mode === 'login';
  const isProfileOnly = mode === 'profile';
  const isRunnerBeginner = form.jaCorre && form.nivelCorrida === 'INICIANTE';
  const isNoRunnerFlow = form.jaCorre === false || isRunnerBeginner;
  const isIntermediate = form.jaCorre && form.nivelCorrida === 'INTERMEDIARIO';
  const isAdvanced = form.jaCorre && form.nivelCorrida === 'AVANCADO';

  const profileSteps = useMemo(() => {
    if (form.jaCorre === null || profileStep < 2) return ['dados', 'historico', 'ramo'];
    if (isNoRunnerFlow) return ['dados', 'historico', 'base', 'objetivo'];
    return ['dados', 'historico', 'corrida', 'performance'];
  }, [form.jaCorre, isNoRunnerFlow, profileStep]);

  useEffect(() => {
    if (isProfileOnly) {
      setForm(buildFormFromUser(user));
      setProfileStep(0);
      return;
    }
    setForm(createEmptyForm());
  }, [isProfileOnly, user]);

  const totalSteps = isProfileOnly ? profileSteps.length : 1;
  const currentKey = profileSteps[Math.min(profileStep, profileSteps.length - 1)];
  const isLastProfileStep = profileStep === profileSteps.length - 1;

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const updateZone = (field, value) => {
    setForm((current) => ({
      ...current,
      zonasPace: { ...current.zonasPace, [field]: value },
    }));
  };

  const isCurrentProfileStepValid = useMemo(() => {
    if (!isProfileOnly) return true;

    if (currentKey === 'dados') {
      return Boolean(form.idade && form.alturaCm && form.pesoKg);
    }

    if (currentKey === 'historico') {
      return form.jaCorre !== null;
    }

    if (currentKey === 'base') {
      return form.fazAtividade !== null && Boolean(form.atividadeNivel && form.horasSonoMedia);
    }

    if (currentKey === 'objetivo') {
      const wants10k = form.objetivoNaoCorredor === '10K';
      return Boolean(
        form.diasSemanaTreino.length > 0 &&
        form.semanasTreino &&
        form.objetivoNaoCorredor &&
        (!wants10k || form.atividadeNivel === 'MUITO_ATIVO')
      );
    }

    if (currentKey === 'corrida') {
      return Boolean(form.objetivoCorredor && form.nivelCorrida);
    }

    if (currentKey === 'performance') {
      const hasBase = Boolean(form.distanciaRp && form.paceRp && form.diasSemanaTreino.length > 0 && form.semanasTreino);
      const hasZones = !isAdvanced || Object.values(form.zonasPace).every(Boolean);
      return hasBase && hasZones;
    }

    return true;
  }, [currentKey, form, isAdvanced, isProfileOnly]);

  const valid = useMemo(() => {
    if (isLogin) return Boolean(form.email && form.senha);
    if (!isProfileOnly) return Boolean(form.email && form.senha.length >= 6);

    return Boolean(
      form.idade &&
      form.alturaCm &&
      form.pesoKg &&
      form.jaCorre !== null &&
      isCurrentProfileStepValid
    );
  }, [form, isCurrentProfileStepValid, isLogin, isProfileOnly]);

  const nextRouteAfterProfileSave = hasCompletedOnboarding ? '/' : '/gerar-plano';

  const submit = async (event) => {
    event.preventDefault();
    if (!valid || loading) return;

    setLoading(true);
    setError('');

    try {
      if (isLogin) {
        const currentUser = await login({ email: form.email, senha: form.senha });
        const shouldCompleteProfile = !currentUser?.objetivo || !currentUser?.nivelCondicionamento;
        navigate(shouldCompleteProfile ? '/onboarding' : '/');
        return;
      }

      if (isProfileOnly) {
        await api.usuarios.atualizarOnboarding(buildProfilePayload(form));
      } else {
        await api.usuarios.registrar({
          nome: form.email.split('@')[0],
          email: form.email,
          senha: form.senha,
        });
        await login({ email: form.email, senha: form.senha });
      }

      await reloadUser();
      navigate(isProfileOnly ? nextRouteAfterProfileSave : '/onboarding');
    } catch (err) {
      setError(err.message || 'Nao foi possivel concluir a operacao.');
    } finally {
      setLoading(false);
    }
  };

  const goToNextProfileStep = () => {
    if (!isCurrentProfileStepValid) {
      setError('Preencha os dados obrigatorios desta etapa para continuar.');
      return;
    }

    setError('');
    setProfileStep((current) => Math.min(current + 1, profileSteps.length - 1));
  };

  const handleGoogle = () => {
    setError('Entrada com Google ainda precisa ser conectada no backend.');
  };

  const renderProgress = () => (
    <div className="mb-8">
      <div className="mb-4 flex gap-1.5">
        {Array.from({ length: totalSteps }).map((_, index) => (
          <div
            key={index}
            className={`h-1 flex-1 rounded-full ${index <= profileStep ? 'bg-primary' : 'bg-secondary'}`}
          />
        ))}
      </div>
      <p className="mb-2 text-xs font-semibold text-primary">Etapa {profileStep + 1} de {totalSteps}</p>
    </div>
  );

  const renderProfileStep = () => (
    <>
      {renderProgress()}

      {currentKey === 'dados' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Dados do atleta</h2>
            <p className="mt-2 text-sm text-muted-foreground">Comece com o basico para calibrar volume e seguranca.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input type="number" min="12" max="90" placeholder="Idade" value={form.idade} onChange={(event) => updateForm('idade', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
            <Input type="number" min="120" max="230" placeholder="Altura cm" value={form.alturaCm} onChange={(event) => updateForm('alturaCm', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
          </div>
          <Input type="number" min="30" max="250" step="0.1" placeholder="Peso kg" value={form.pesoKg} onChange={(event) => updateForm('pesoKg', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
        </section>
      )}

      {currentKey === 'historico' && (
        <section className="space-y-5">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Voce ja corre?</h2>
            <p className="mt-2 text-sm text-muted-foreground">Isso define se o plano comeca pela base ou pela performance.</p>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectionCard label="Nao corro" description="Quero comecar com progressao segura" selected={form.jaCorre === false} onClick={() => { updateForm('jaCorre', false); setProfileStep(2); }} />
            <SelectionCard label="Ja corro" description="Tenho alguma experiencia com corrida" selected={form.jaCorre === true} onClick={() => { updateForm('jaCorre', true); setProfileStep(2); }} />
          </div>
        </section>
      )}

      {currentKey === 'base' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Sua base atual</h2>
            <p className="mt-2 text-sm text-muted-foreground">Essas respostas ajudam a evitar uma carga agressiva demais.</p>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Faz outras atividades fisicas?</p>
            <div className="grid grid-cols-2 gap-3">
              <SelectionCard label="Sim" selected={form.fazAtividade === true} onClick={() => updateForm('fazAtividade', true)} />
              <SelectionCard label="Nao" selected={form.fazAtividade === false} onClick={() => updateForm('fazAtividade', false)} />
            </div>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Quao ativa e sua rotina?</p>
            <div className="space-y-2">
              <SelectionCard label="Pouco ativa" description="Passo boa parte do dia sentado" selected={form.atividadeNivel === 'POUCO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'POUCO_ATIVO')} />
              <SelectionCard label="Ativa" description="Caminho, treino ou me movimento com frequencia" selected={form.atividadeNivel === 'ATIVO'} onClick={() => updateForm('atividadeNivel', 'ATIVO')} />
              <SelectionCard label="Muito ativa" description="Treino outras modalidades com boa consistencia" selected={form.atividadeNivel === 'MUITO_ATIVO'} onClick={() => updateForm('atividadeNivel', 'MUITO_ATIVO')} />
            </div>
          </div>
          <Input type="number" min="3" max="12" placeholder="Horas de sono por noite" value={form.horasSonoMedia} onChange={(event) => updateForm('horasSonoMedia', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
        </section>
      )}

      {currentKey === 'objetivo' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Seu primeiro objetivo</h2>
            <p className="mt-2 text-sm text-muted-foreground">Escolha a distancia e o tempo de preparacao.</p>
          </div>
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponiveis por semana</p>
            <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
          </div>
          <Input type="number" min="4" max="24" placeholder="Semanas de treinamento (4 a 24)" value={form.semanasTreino} onChange={(event) => updateForm('semanasTreino', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
          <div className="space-y-2">
            {DISTANCE_GOALS.map((item) => {
              const disabled = item.value === '10K' && form.atividadeNivel !== 'MUITO_ATIVO';
              return (
                <SelectionCard
                  key={item.value}
                  label={item.recommended ? `${item.label} - recomendado` : item.label}
                  description={disabled ? '10 km fica liberado para quem marcou rotina muito ativa' : item.description}
                  selected={form.objetivoNaoCorredor === item.value}
                  onClick={() => !disabled && updateForm('objetivoNaoCorredor', item.value)}
                />
              );
            })}
          </div>
        </section>
      )}

      {currentKey === 'corrida' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">Meta de corrida</h2>
            <p className="mt-2 text-sm text-muted-foreground">Defina seu objetivo e nivel para o plano ajustar volume e intensidade.</p>
          </div>
          <div className="space-y-2">
            {RUNNER_GOALS.map((item) => (
              <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.objetivoCorredor === item.value} onClick={() => updateForm('objetivoCorredor', item.value)} />
            ))}
          </div>
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Nivel na corrida</p>
            {LEVELS.map((item) => (
              <SelectionCard key={item.value} label={item.label} description={item.description} selected={form.nivelCorrida === item.value} onClick={() => updateForm('nivelCorrida', item.value)} />
            ))}
          </div>
        </section>
      )}

      {currentKey === 'performance' && (
        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-bold text-foreground">{isIntermediate ? 'Seu melhor ritmo' : 'Dados avancados'}</h2>
            <p className="mt-2 text-sm text-muted-foreground">Use uma referencia real para a IA estimar treinos mais precisos.</p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {RP_DISTANCES.map((item) => (
              <SelectionCard key={item.value} label={item.label} selected={form.distanciaRp === item.value} onClick={() => updateForm('distanciaRp', item.value)} />
            ))}
          </div>
          <Input placeholder="Pace do RP (ex.: 5.30 min/km)" value={form.paceRp} onChange={(event) => updateForm('paceRp', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase text-muted-foreground">Dias disponiveis para treinar</p>
            <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
          </div>
          <Input type="number" min="4" max="36" placeholder="Semanas ate o objetivo" value={form.semanasTreino} onChange={(event) => updateForm('semanasTreino', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
          {isAdvanced && (
            <div className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div>
                <p className="text-xs font-semibold uppercase text-muted-foreground">Paces por zona</p>
                <p className="mt-1 text-xs text-muted-foreground">Preencha Z1 ate Z5 no formato min/km.</p>
              </div>
              <div className="grid grid-cols-2 gap-3">
                {['z1', 'z2', 'z3', 'z4', 'z5'].map((zone) => (
                  <Input key={zone} placeholder={zone.toUpperCase()} value={form.zonasPace[zone]} onChange={(event) => updateZone(zone, event.target.value)} className="h-11 rounded-xl border-border bg-background" />
                ))}
              </div>
            </div>
          )}
        </section>
      )}
    </>
  );

  return (
    <div className="min-h-screen bg-background px-5 py-8">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg flex-col">
        <div className="mb-8">
          <Link to="/" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-primary">
            <ChevronLeft className="h-4 w-4" />
            SeuCorre
          </Link>
          <div className="rounded-3xl border border-border bg-card/80 p-5 shadow-2xl shadow-black/20">
            <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/15 text-primary">
              {isLogin ? <LogIn className="h-6 w-6" /> : isProfileOnly ? <ShieldCheck className="h-6 w-6" /> : <Mail className="h-6 w-6" />}
            </div>
            <h1 className="text-3xl font-bold text-foreground">
              {isLogin ? 'Entrar' : isProfileOnly ? 'Perfil do atleta' : 'Criar conta'}
            </h1>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {isLogin
                ? 'Entre com email e senha ou continue pelo Google.'
                : isProfileOnly
                  ? 'Responda o necessario para o SeuCorre montar um plano coerente com seu momento.'
                  : 'Crie sua conta com email e senha ou continue pelo Google.'}
            </p>
          </div>
        </div>

        <form onSubmit={submit} className="flex flex-1 flex-col gap-5 pb-28">
          {!isProfileOnly && (
            <>
              <GoogleButton label={isLogin ? 'Entrar com Google' : 'Criar conta com Google'} onClick={handleGoogle} />
              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                <span className="h-px flex-1 bg-border" />
                ou
                <span className="h-px flex-1 bg-border" />
              </div>
            </>
          )}

          {!isLogin && !isProfileOnly && (
            <div className="space-y-3">
              <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
            </div>
          )}

          {isLogin && (
            <div className="space-y-3">
              <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="h-12 rounded-xl border-border bg-card" />
              <Link to="/cadastro" className="block text-center text-sm font-medium text-primary">Criar uma conta</Link>
            </div>
          )}

          {isProfileOnly && renderProfileStep()}

          {!isLogin && !isProfileOnly && (
            <Link to="/entrar" className="block text-center text-sm font-medium text-primary">Ja tenho conta</Link>
          )}

          {error && <p className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}

          <div className="fixed bottom-0 left-0 right-0 mx-auto max-w-lg bg-gradient-to-t from-background via-background to-transparent p-5">
            {isProfileOnly ? (
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={loading || profileStep === 0}
                  onClick={() => {
                    setError('');
                    setProfileStep((current) => Math.max(current - 1, 0));
                  }}
                  className="h-14 rounded-2xl px-5"
                >
                  <ChevronLeft className="h-5 w-5" />
                  Voltar
                </Button>
                {isLastProfileStep ? (
                  <Button type="submit" disabled={!valid || loading} className="h-14 flex-1 rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                    {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : <span className="flex items-center gap-2"><Sparkles className="h-5 w-5" />Finalizar<ArrowRight className="h-5 w-5" /></span>}
                  </Button>
                ) : (
                  <Button type="button" onClick={goToNextProfileStep} className="h-14 flex-1 rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                    Continuar
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                )}
              </div>
            ) : (
              <Button type="submit" disabled={!valid || loading} className="h-14 w-full rounded-2xl bg-primary text-base font-semibold text-primary-foreground">
                {loading ? (
                  <Loader2 className="h-5 w-5 animate-spin" />
                ) : isLogin ? (
                  <span className="flex items-center gap-2"><LogIn className="h-5 w-5" />Entrar</span>
                ) : (
                  <span className="flex items-center gap-2"><Sparkles className="h-5 w-5" />Criar conta<ArrowRight className="h-5 w-5" /></span>
                )}
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
