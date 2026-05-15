import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronLeft, Loader2, LogIn, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/AuthContext';
import DaySelector from '@/components/onboarding/DaySelector';
import SelectionCard from '@/components/onboarding/SelectionCard';
import {
  GOAL_OPTIONS,
  GENDER_OPTIONS,
  LEVEL_OPTIONS,
  ageFromBirthDate,
  birthDateFromAge,
  parseTrainingDays,
  serializeTrainingDays,
} from '@/lib/user';

const HEALTH_OPTIONS = [
  { value: 'DOR_RECORRENTE', label: 'Dor recorrente', description: 'Exige mais cuidado com volume e impacto' },
  { value: 'ASMA', label: 'Asma', description: 'Ajuda a IA a ajustar intensidade e progressão' },
  { value: 'HIPERTENSAO', label: 'Hipertensão', description: 'Importante para orientar esforço e FC' },
  { value: 'LESAO_ANTERIOR', label: 'Lesão anterior', description: 'Evita progressões agressivas' },
];

const WEARABLE_OPTIONS = [
  { value: 'GARMIN', label: 'Garmin', description: 'Sincronização futura com Garmin Connect' },
  { value: 'POLAR', label: 'Polar', description: 'Sincronização futura com Polar Flow' },
  { value: 'APPLE_WATCH', label: 'Apple Watch', description: 'Sincronização futura com Apple Health' },
  { value: 'STRAVA', label: 'Strava', description: 'Importação de treinos e histórico' },
];

const PROFILE_STEPS = [
  {
    key: 'objetivo',
    eyebrow: 'Etapa 1 de 5',
    title: 'Seu momento atual',
    subtitle: 'Defina nível e objetivo para a IA calibrar a carga inicial.',
  },
  {
    key: 'rotina',
    eyebrow: 'Etapa 2 de 5',
    title: 'Sua rotina de treino',
    subtitle: 'Escolha quantos dias você consegue treinar e seu histórico com corrida.',
  },
  {
    key: 'dados',
    eyebrow: 'Etapa 3 de 5',
    title: 'Dados essenciais',
    subtitle: 'Só o necessário para gerar um plano seguro. Métricas avançadas ficam como opcionais.',
  },
  {
    key: 'saude',
    eyebrow: 'Etapa 4 de 5',
    title: 'Saúde e contexto',
    subtitle: 'Opcional, mas ajuda a reduzir risco e evitar treinos fora do seu momento.',
  },
  {
    key: 'wearable',
    eyebrow: 'Etapa 5 de 5',
    title: 'Wearables e integração',
    subtitle: 'Conectar agora é opcional. Você pode finalizar o onboarding e fazer isso depois.',
  },
];

const createEmptyForm = () => ({
  nome: '',
  email: '',
  senha: '',
  telefone: '',
  nivelCondicionamento: 'INICIANTE',
  objetivo: 'SAUDE_GERAL',
  jaCorre: false,
  diasSemanaTreino: ['seg', 'qua', 'sex'],
  idade: '',
  pesoKg: '',
  alturaCm: '',
  genero: '',
  fcRepouso: '',
  fcMaxima: '',
  horasSonoMedia: '',
  condicoesSaude: [],
  observacaoSaude: '',
  wearablePlataforma: '',
  wearableToken: '',
});

const buildFormFromUser = (user) => {
  const days = parseTrainingDays(user?.diasSemanaTreino);
  return {
    nome: user?.nome || '',
    email: user?.email || '',
    senha: '',
    telefone: '',
    nivelCondicionamento: user?.nivelCondicionamento || 'INICIANTE',
    objetivo: user?.objetivo || 'SAUDE_GERAL',
    jaCorre: Boolean(user?.jaCorre),
    diasSemanaTreino: days.length > 0 ? days : ['seg', 'qua', 'sex'],
    idade: ageFromBirthDate(user?.dataNascimento),
    pesoKg: user?.pesoKg != null ? String(user.pesoKg) : '',
    alturaCm: user?.alturaCm != null ? String(user.alturaCm) : '',
    genero: user?.genero || '',
    fcRepouso: user?.fcRepouso != null ? String(user.fcRepouso) : '',
    fcMaxima: user?.fcMaxima != null ? String(user.fcMaxima) : '',
    horasSonoMedia: user?.horasSonoMedia != null ? String(user.horasSonoMedia) : '',
    condicoesSaude: [],
    observacaoSaude: '',
    wearablePlataforma: '',
    wearableToken: '',
  };
};

const toOptionalInteger = (value) => {
  if (value === '' || value == null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const buildCondicoesSaude = (form) => (
  (form.condicoesSaude || []).map((tipo) => ({
    tipo,
    descricao: form.observacaoSaude || null,
    ativa: true,
  }))
);

const buildDispositivos = (form) => {
  if (!form.wearablePlataforma || !form.wearableToken) {
    return [];
  }

  return [{
    plataforma: form.wearablePlataforma,
    tokenAcesso: form.wearableToken,
    // TODO: substituir por token real e expiração real quando a integração OAuth do wearable existir.
    tokenExpiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19),
  }];
};

const buildProfilePayload = (form) => ({
  telefone: form.telefone || null,
  dadosFisicos: {
    pesoKg: Number(form.pesoKg),
    alturaCm: Number(form.alturaCm),
    dataNascimento: birthDateFromAge(form.idade),
    genero: form.genero,
    fcRepouso: toOptionalInteger(form.fcRepouso),
    fcMaxima: toOptionalInteger(form.fcMaxima),
    horasSonoMedia: toOptionalInteger(form.horasSonoMedia),
    sedentario: form.nivelCondicionamento === 'INICIANTE',
  },
  perfilAtleta: {
    nivelCondicionamento: form.nivelCondicionamento,
    objetivo: form.objetivo,
    jaCorre: form.jaCorre,
    diasDisponiveisSemana: form.diasSemanaTreino.length,
    diasSemanaTreino: serializeTrainingDays(form.diasSemanaTreino),
  },
  perfilCorrida: null,
  condicoesSaude: buildCondicoesSaude(form),
  dispositivos: buildDispositivos(form),
});

export default function AuthPage({ mode = 'register' }) {
  const navigate = useNavigate();
  const { hasCompletedOnboarding, login, reloadUser, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(createEmptyForm);
  const [profileStep, setProfileStep] = useState(0);

  const isLogin = mode === 'login';
  const isProfileOnly = mode === 'profile';

  useEffect(() => {
    if (isProfileOnly) {
      setForm(buildFormFromUser(user));
      setProfileStep(0);
      return;
    }
    setForm(createEmptyForm());
  }, [isProfileOnly, user]);

  const isCurrentProfileStepValid = useMemo(() => {
    if (!isProfileOnly) return true;

    switch (profileStep) {
      case 0:
        return Boolean(form.nivelCondicionamento && form.objetivo);
      case 1:
        return Boolean(form.diasSemanaTreino.length > 0);
      case 2:
        return Boolean(form.idade && form.pesoKg && form.alturaCm && form.genero);
      case 3:
        return true;
      case 4:
        return !form.wearablePlataforma || Boolean(form.wearableToken);
      default:
        return true;
    }
  }, [form, isProfileOnly, profileStep]);

  const valid = useMemo(() => {
    if (isLogin) return Boolean(form.email && form.senha);
    if (!isProfileOnly) {
      return Boolean(form.nome && form.email && form.senha.length >= 6);
    }
    return Boolean(
      form.idade &&
      form.pesoKg &&
      form.alturaCm &&
      form.genero &&
      form.diasSemanaTreino.length > 0 &&
      form.nivelCondicionamento &&
      form.objetivo &&
      isCurrentProfileStepValid
    );
  }, [form, isLogin, isProfileOnly, isCurrentProfileStepValid]);

  const nextRouteAfterProfileSave = hasCompletedOnboarding ? '/' : '/gerar-plano';

  const updateForm = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const toggleHealthCondition = (value) => setForm((current) => ({
    ...current,
    condicoesSaude: current.condicoesSaude.includes(value)
      ? current.condicoesSaude.filter((item) => item !== value)
      : [...current.condicoesSaude, value],
  }));
  const isLastProfileStep = profileStep === PROFILE_STEPS.length - 1;

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
          nome: form.nome,
          email: form.email,
          senha: form.senha,
        });
        await login({ email: form.email, senha: form.senha });
      }

      await reloadUser();
      navigate(isProfileOnly ? nextRouteAfterProfileSave : '/onboarding');
    } catch (err) {
      setError(err.message || 'Não foi possível concluir a operação.');
    } finally {
      setLoading(false);
    }
  };

  const goToNextProfileStep = () => {
    if (!isCurrentProfileStepValid) {
      setError('Preencha os dados obrigatórios desta etapa para continuar.');
      return;
    }
    setError('');
    setProfileStep((current) => Math.min(current + 1, PROFILE_STEPS.length - 1));
  };

  const renderProfileStep = () => {
    const step = PROFILE_STEPS[profileStep];

    return (
      <>
        <div className="mb-6">
          <div className="flex gap-1.5 mb-4">
            {PROFILE_STEPS.map((item, index) => (
              <div
                key={item.key}
                className={`h-1 rounded-full flex-1 ${index <= profileStep ? 'bg-primary' : 'bg-secondary'}`}
              />
            ))}
          </div>
          <p className="text-xs text-primary font-semibold mb-2">{step.eyebrow}</p>
          <h2 className="text-2xl font-bold text-foreground">{step.title}</h2>
          <p className="text-sm text-muted-foreground mt-2">{step.subtitle}</p>
        </div>

        {profileStep === 0 && (
          <div className="space-y-6">
            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Nível</h3>
                <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
              </div>
              <div className="space-y-2">
                {LEVEL_OPTIONS.map((item) => (
                  <SelectionCard
                    key={item.value}
                    label={item.label}
                    description={item.description}
                    selected={form.nivelCondicionamento === item.value}
                    onClick={() => updateForm('nivelCondicionamento', item.value)}
                  />
                ))}
              </div>
            </section>

            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Objetivo</h3>
                <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
              </div>
              <div className="grid grid-cols-1 gap-2">
                {GOAL_OPTIONS.map((item) => (
                  <SelectionCard
                    key={item.value}
                    label={item.label}
                    description={item.description}
                    selected={form.objetivo === item.value}
                    onClick={() => updateForm('objetivo', item.value)}
                  />
                ))}
              </div>
            </section>
          </div>
        )}

        {profileStep === 1 && (
          <div className="space-y-6">
            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Histórico</h3>
                <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <SelectionCard
                  label="Estou começando"
                  description="Prefiro clareza e volume inicial menor"
                  selected={!form.jaCorre}
                  onClick={() => updateForm('jaCorre', false)}
                />
                <SelectionCard
                  label="Já corro"
                  description="Quero evoluir com base no que já faço hoje"
                  selected={form.jaCorre}
                  onClick={() => updateForm('jaCorre', true)}
                />
              </div>
            </section>

            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Dias de treino</h3>
                <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
              </div>
              <p className="text-xs text-muted-foreground mb-3">Escolha os dias em que você realmente consegue treinar.</p>
              <DaySelector selected={form.diasSemanaTreino} onChange={(dias) => updateForm('diasSemanaTreino', dias)} />
            </section>
          </div>
        )}

        {profileStep === 2 && (
          <div className="space-y-6">
            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Dados essenciais</h3>
                <span className="text-[10px] font-semibold text-primary">Obrigatório</span>
              </div>
              <Input placeholder="Telefone (opcional)" value={form.telefone} onChange={(event) => updateForm('telefone', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
              <div className="grid grid-cols-2 gap-3">
                <Input type="number" placeholder="Idade" value={form.idade} onChange={(event) => updateForm('idade', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
                <Input type="number" step="0.1" placeholder="Peso kg" value={form.pesoKg} onChange={(event) => updateForm('pesoKg', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
              </div>
              <Input type="number" step="0.1" placeholder="Altura cm" value={form.alturaCm} onChange={(event) => updateForm('alturaCm', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
              <div className="grid grid-cols-2 gap-3">
                {GENDER_OPTIONS.map((item) => (
                  <SelectionCard
                    key={item.value}
                    label={item.label}
                    selected={form.genero === item.value}
                    onClick={() => updateForm('genero', item.value)}
                  />
                ))}
              </div>
            </section>

            <section className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Métricas avançadas</h3>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <p className="text-xs text-muted-foreground">Preencha só se souber. Isso ajuda a IA a refinar o plano sem travar seu onboarding.</p>
              <div className="grid grid-cols-3 gap-3">
                <Input type="number" placeholder="FC repouso" value={form.fcRepouso} onChange={(event) => updateForm('fcRepouso', event.target.value)} className="bg-background border-border h-12 rounded-xl" />
                <Input type="number" placeholder="FC máxima" value={form.fcMaxima} onChange={(event) => updateForm('fcMaxima', event.target.value)} className="bg-background border-border h-12 rounded-xl" />
                <Input type="number" placeholder="Sono médio" value={form.horasSonoMedia} onChange={(event) => updateForm('horasSonoMedia', event.target.value)} className="bg-background border-border h-12 rounded-xl" />
              </div>
            </section>
          </div>
        )}

        {profileStep === 3 && (
          <div className="space-y-6">
            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Condições de saúde</h3>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <div className="space-y-2">
                {HEALTH_OPTIONS.map((item) => (
                  <SelectionCard
                    key={item.value}
                    label={item.label}
                    description={item.description}
                    selected={form.condicoesSaude.includes(item.value)}
                    onClick={() => toggleHealthCondition(item.value)}
                  />
                ))}
              </div>
            </section>

            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Observações</h3>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <Textarea
                placeholder="Ex.: voltando de lesão, dor no joelho ao aumentar volume, asma controlada..."
                value={form.observacaoSaude}
                onChange={(event) => updateForm('observacaoSaude', event.target.value)}
                className="min-h-[120px] rounded-xl border-border bg-card"
              />
            </section>
          </div>
        )}

        {profileStep === 4 && (
          <div className="space-y-6">
            <section>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Wearable</h3>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <div className="space-y-2">
                {WEARABLE_OPTIONS.map((item) => (
                  <SelectionCard
                    key={item.value}
                    label={item.label}
                    description={item.description}
                    selected={form.wearablePlataforma === item.value}
                    onClick={() => updateForm('wearablePlataforma', form.wearablePlataforma === item.value ? '' : item.value)}
                  />
                ))}
              </div>
            </section>

            <section className="space-y-3 rounded-2xl border border-border bg-card p-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">Conexão agora</h3>
                <span className="text-[10px] font-semibold text-muted-foreground">Opcional</span>
              </div>
              <p className="text-xs text-muted-foreground">
                Se você já tiver um token de integração, pode informar agora. Caso contrário, finalize o onboarding e conecte depois no perfil.
              </p>
              <Input
                placeholder="Token/código de integração"
                value={form.wearableToken}
                onChange={(event) => updateForm('wearableToken', event.target.value)}
                className="bg-background border-border h-12 rounded-xl"
              />
            </section>
          </div>
        )}
      </>
    );
  };

  return (
    <div className="min-h-screen bg-background max-w-lg mx-auto px-5 py-10">
      <div className="mb-8">
        <p className="text-xs text-primary font-semibold mb-2">SeuCorre</p>
        <h1 className="text-3xl font-bold text-foreground">
          {isLogin ? 'Entrar' : isProfileOnly ? 'Atualizar perfil' : 'Criar conta'}
        </h1>
        <p className="text-sm text-muted-foreground mt-2">
          {isLogin
            ? 'Entre com seu e-mail e senha para continuar.'
            : isProfileOnly
              ? 'Atualize seus dados para manter o plano coerente com seu momento atual.'
              : 'Crie sua conta. O onboarding vem no passo seguinte.'}
        </p>
      </div>

      <form onSubmit={submit} className="space-y-5 pb-32">
        {!isLogin && !isProfileOnly && (
          <div className="space-y-3">
            <Input placeholder="Nome" value={form.nome} onChange={(event) => updateForm('nome', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
            <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
            <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
          </div>
        )}

        {isLogin ? (
          <div className="space-y-3">
            <Input type="email" placeholder="E-mail" value={form.email} onChange={(event) => updateForm('email', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
            <Input type="password" placeholder="Senha" value={form.senha} onChange={(event) => updateForm('senha', event.target.value)} className="bg-card border-border h-12 rounded-xl" />
            <Link to="/cadastro" className="block w-full text-center text-sm text-primary">Criar uma conta</Link>
          </div>
        ) : isProfileOnly ? (
          renderProfileStep()
        ) : (
          <p className="rounded-2xl border border-border bg-card p-4 text-sm text-muted-foreground">
            Depois de criar a conta, o sistema leva você para o onboarding.
          </p>
        )}

        {error && <p className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}

        {!isLogin && !isProfileOnly && (
          <Link to="/entrar" className="block text-center text-sm text-primary">Já tenho conta</Link>
        )}

        <div className="fixed bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-background via-background to-transparent max-w-lg mx-auto">
          {isProfileOnly ? (
            <div className="flex gap-3">
              <Button
                type="button"
                variant="outline"
                disabled={loading || profileStep === 0}
                onClick={() => setProfileStep((current) => Math.max(current - 1, 0))}
                className="h-14 rounded-2xl px-5"
              >
                <ChevronLeft className="w-5 h-5" />
                Voltar
              </Button>
              {isLastProfileStep ? (
                <Button type="submit" disabled={!valid || loading} className="flex-1 h-14 rounded-2xl bg-primary text-primary-foreground font-semibold text-base">
                  {loading ? (
                    <Loader2 className="w-5 h-5 animate-spin" />
                  ) : (
                    <span className="flex items-center gap-2">
                      <Sparkles className="w-5 h-5" />
                      Finalizar onboarding
                      <ArrowRight className="w-5 h-5" />
                    </span>
                  )}
                </Button>
              ) : (
                <Button type="button" onClick={goToNextProfileStep} className="flex-1 h-14 rounded-2xl bg-primary text-primary-foreground font-semibold text-base">
                  Continuar
                  <ArrowRight className="w-5 h-5" />
                </Button>
              )}
            </div>
          ) : (
            <Button type="submit" disabled={!valid || loading} className="w-full h-14 rounded-2xl bg-primary text-primary-foreground font-semibold text-base">
              {loading ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : isLogin ? (
                <span className="flex items-center gap-2"><LogIn className="w-5 h-5" />Entrar</span>
              ) : (
                <span className="flex items-center gap-2">
                  <Sparkles className="w-5 h-5" />
                  Criar conta
                  <ArrowRight className="w-5 h-5" />
                </span>
              )}
            </Button>
          )}
        </div>
      </form>
    </div>
  );
}
