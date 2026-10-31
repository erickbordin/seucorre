import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { api, auth, onSessionExpired, tokenStore } from '@/lib/api';
import { clearOnboardingDraft, hasCompletedOnboarding as checkOnboardingCompletion } from '@/lib/user';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const queryClient = useQueryClient();
  const [user, setUser] = useState(null);
  const [isLoadingAuth, setIsLoadingAuth] = useState(true);
  const [authError, setAuthError] = useState(null);

  const loadUser = async () => {
    if (!tokenStore.getAccessToken()) {
      setIsLoadingAuth(false);
      setUser(null);
      return null;
    }

    try {
      setIsLoadingAuth(true);
      const currentUser = await api.usuarios.me();
      setUser(currentUser);
      setAuthError(null);
      return currentUser;
    } catch (error) {
      if (error.status === 401) tokenStore.clear();
      setUser(null);
      setAuthError(error);
      return null;
    } finally {
      setIsLoadingAuth(false);
    }
  };

  useEffect(() => {
    loadUser();
    return onSessionExpired(() => {
      queryClient.clear();
      setUser(null);
    });
  }, []);

  const login = async ({ email, senha }) => {
    await auth.login(email, senha);
    return loadUser();
  };

  const logout = () => {
    auth.logout();
    clearOnboardingDraft();
    queryClient.clear();
    setUser(null);
    setAuthError(null);
  };

  const isAuthenticated = Boolean(user);
  const hasCompletedOnboarding = checkOnboardingCompletion(user);

  const value = useMemo(() => ({
    user,
    isAuthenticated,
    hasCompletedOnboarding,
    isLoadingAuth,
    isLoadingPublicSettings: false,
    authError,
    login,
    logout,
    reloadUser: loadUser,
  }), [user, isAuthenticated, hasCompletedOnboarding, isLoadingAuth, authError]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
