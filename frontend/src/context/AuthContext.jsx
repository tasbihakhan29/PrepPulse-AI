import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/auth';
import toast from 'react-hot-toast';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Fetch current user on load if token exists
  useEffect(() => {
    const fetchUser = async () => {
      const token = localStorage.getItem('token');
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const userData = await authApi.getMe();
        setUser(userData);
      } catch (error) {
        console.error('Failed to load user session:', error);
        localStorage.removeItem('token');
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, []);

  const signup = async (name, email, password) => {
    setLoading(true);
    try {
      const data = await authApi.signup(name, email, password);
      localStorage.setItem('token', data.token);
      setUser(data.user);
      toast.success('Registration successful!');
      return data.user;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Registration failed. Please try again.';
      toast.error(errorMsg);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    setLoading(true);
    try {
      const data = await authApi.login(email, password);
      localStorage.setItem('token', data.token);
      setUser(data.user);
      toast.success('Welcome back!');
      return data.user;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Login failed. Please try again.';
      toast.error(errorMsg);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const googleLogin = async (idToken) => {
    setLoading(true);
    try {
      const data = await authApi.googleLogin(idToken);
      localStorage.setItem('token', data.token);
      setUser(data.user);
      toast.success('Signed in with Google!');
      return data.user;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Google Sign-In failed.';
      toast.error(errorMsg);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
    toast.success('Logged out successfully.');
  };

  const forgotPassword = async (email) => {
    try {
      const response = await authApi.forgotPassword(email);
      toast.success(response.message || 'OTP verification code sent!');
      return response;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Failed to request OTP code.';
      toast.error(errorMsg);
      throw error;
    }
  };

  const resetPassword = async (email, otpCode, password) => {
    try {
      const response = await authApi.resetPassword(email, otpCode, password);
      toast.success(response.message || 'Password reset successful!');
      return response;
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Failed to reset password.';
      toast.error(errorMsg);
      throw error;
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        signup,
        login,
        googleLogin,
        logout,
        forgotPassword,
        resetPassword,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
