import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/auth/LoginPage';
import SignupPage from './pages/auth/SignupPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import PracticeCenterPage from './pages/PracticeCenterPage';
import AnswerEvaluatorPage from './pages/AnswerEvaluatorPage';
import TestTakingPage from './pages/TestTakingPage';
import ResultPage from './pages/ResultPage';
import HistoryAnalyticsPage from './pages/HistoryAnalyticsPage';

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

function App() {
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

  return (
    <GoogleOAuthProvider clientId={googleClientId}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            
            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              } 
            />
            
            <Route 
              path="/practice-center" 
              element={
                <ProtectedRoute>
                  <PracticeCenterPage />
                </ProtectedRoute>
              } 
            />
            
            <Route 
              path="/answer-evaluator" 
              element={
                <ProtectedRoute>
                  <AnswerEvaluatorPage />
                </ProtectedRoute>
              } 
            />
            
            <Route 
              path="/test/:testId" 
              element={
                <ProtectedRoute>
                  <TestTakingPage />
                </ProtectedRoute>
              } 
            />
            
            <Route 
              path="/result/:attemptId" 
              element={
                <ProtectedRoute>
                  <ResultPage />
                </ProtectedRoute>
              } 
            />

            <Route 
              path="/history-analytics" 
              element={
                <ProtectedRoute>
                  <HistoryAnalyticsPage />
                </ProtectedRoute>
              } 
            />
            
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
        <Toaster position="top-right" />
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
