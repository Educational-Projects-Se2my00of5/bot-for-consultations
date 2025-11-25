import React, { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import UsersPage from './pages/UsersPage';
import DeaneryPage from './pages/DeaneryPage';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

function App() {
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkToken = async () => {
      const token = localStorage.getItem('adminToken');
      
      if (token) {
        try {
          const response = await fetch(`${API_URL}/api/admin/check-token`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ token }),
          });

          if (!response.ok) {
            localStorage.removeItem('adminToken');
          }
        } catch (error) {
          console.error('Ошибка при проверке токена:', error);
          localStorage.removeItem('adminToken');
        }
      }
      setIsLoading(false);
    };

    checkToken();
  }, []);

  if (isLoading) {
    return <div>Загрузка...</div>;
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to="/users" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route 
          path="/users" 
          element={
            <ProtectedRoute>
              <UsersPage />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/deanery" 
          element={
            <ProtectedRoute>
              <DeaneryPage />
            </ProtectedRoute>
          } 
        />
      </Routes>
    </Router>
  );
}

export default App;