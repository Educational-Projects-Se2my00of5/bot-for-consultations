import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import InactiveUsersPage from './pages/InactiveUsersPage';
import ActiveUsersPage from './pages/ActiveUsersPage';
import { checkToken } from './api/adminApi';

function App() {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const validateToken = async () => {
      const token = localStorage.getItem('adminToken');
      
      if (token) {
        try {
          const isValid = await checkToken(token);
          if (!isValid) {
            localStorage.removeItem('adminToken');
          }
        } catch (error) {
          console.error('Ошибка при проверке токена:', error);
          localStorage.removeItem('adminToken');
        }
      }
      setIsLoading(false);
    };

    validateToken();
  }, []);

  if (isLoading) {
    return <div>Загрузка...</div>;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to="/inactive" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route 
          path="/inactive" 
          element={
            <ProtectedRoute>
              <InactiveUsersPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/active" 
          element={
            <ProtectedRoute>
              <ActiveUsersPage />
            </ProtectedRoute>
          } 
        />
      </Routes>
    </Router>
  );
}

export default App;