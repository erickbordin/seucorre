import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom';
import { Toaster } from '@/components/ui/toaster';
import { AuthProvider, useAuth } from '@/lib/AuthContext';
import AppLayout from '@/components/layout/AppLayout';
import PageNotFound from '@/lib/PageNotFound';
import LandingPage from '@/page/backend/LandingPage';
import Home from '@/page/backend/Home';
import AuthPage from '@/page/backend/AuthPage';
import GeneratePlan from '@/page/backend/GeneratePlan';
import WorkoutDetail from '@/page/backend/WorkoutDetail';
import Plan from '@/page/backend/Plan';
import Progress from '@/page/backend/Progress';
import Profile from '@/page/backend/Profile';
import CheckIn from '@/page/backend/CheckIn';
import History from '@/page/backend/History';
import Notifications from '@/page/backend/Notifications';
import Paywall from '@/page/Paywall';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

const Loading = () => (
  <div className="fixed inset-0 flex items-center justify-center bg-background">
    <div className="flex flex-col items-center gap-3">
      <div className="w-8 h-8 border-2 border-primary/30 border-t-primary rounded-full animate-spin" />
      <p className="text-xs text-muted-foreground">Carregando...</p>
    </div>
  </div>
);

const AuthRouteRedirect = () => {
  const { hasCompletedOnboarding } = useAuth();
  return <Navigate to={hasCompletedOnboarding ? '/' : '/onboarding'} replace />;
};

const RequireAuth = ({ children, requireOnboarding = false }) => {
  const { isAuthenticated, hasCompletedOnboarding } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/entrar" replace />;
  }

  if (requireOnboarding && !hasCompletedOnboarding) {
    return <Navigate to="/onboarding" replace />;
  }

  return children;
};

const AuthenticatedRoutes = () => {
  const { isLoadingAuth, isAuthenticated, authError, reloadUser } = useAuth();

  if (isLoadingAuth) return <Loading />;

  if (authError && authError.status !== 401) {
    return (
      <div className="fixed inset-0 flex flex-col items-center justify-center gap-4 bg-background px-8 text-center">
        <p className="text-sm text-muted-foreground">Não foi possível conectar ao servidor.</p>
        <button onClick={reloadUser} className="h-11 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground">
          Tentar de novo
        </button>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/" element={isAuthenticated ? (
        <RequireAuth requireOnboarding>
          <AppLayout />
        </RequireAuth>
      ) : <LandingPage />}>
        <Route index element={<Home />} />
      </Route>
      <Route path="/entrar" element={isAuthenticated ? <AuthRouteRedirect /> : <AuthPage mode="login" />} />
      <Route path="/cadastro" element={isAuthenticated ? <AuthRouteRedirect /> : <AuthPage mode="register" />} />
      <Route path="/onboarding" element={
        <RequireAuth>
          <AuthPage mode="profile" />
        </RequireAuth>
      } />
      <Route path="/gerar-plano" element={
        <RequireAuth requireOnboarding>
          <GeneratePlan />
        </RequireAuth>
      } />
      <Route path="/treino/:planoId/:sessionIdx" element={
        <RequireAuth requireOnboarding>
          <WorkoutDetail />
        </RequireAuth>
      } />
      <Route path="/check-in" element={
        <RequireAuth requireOnboarding>
          <CheckIn />
        </RequireAuth>
      } />
      <Route path="/historico" element={
        <RequireAuth requireOnboarding>
          <History />
        </RequireAuth>
      } />
      <Route path="/notificacoes" element={
        <RequireAuth requireOnboarding>
          <Notifications />
        </RequireAuth>
      } />
      <Route path="/paywall" element={<Paywall />} />
      <Route element={
        <RequireAuth requireOnboarding>
          <AppLayout />
        </RequireAuth>
      }>
        <Route path="/plano" element={<Plan />} />
        <Route path="/ia" element={<Navigate to="/plano" replace />} />
        <Route path="/progresso" element={<Progress />} />
        <Route path="/perfil" element={<Profile />} />
      </Route>
      <Route path="*" element={<PageNotFound />} />
    </Routes>
  );
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Router>
          <AuthenticatedRoutes />
        </Router>
        <Toaster />
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
