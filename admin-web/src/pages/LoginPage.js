import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import './LoginPage.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const LoginPage = () => {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('adminToken');
    if (token) {
      navigate('/users');
    }
  }, [navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await fetch(`${API_URL}/api/admin/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ login, password }),
      });

      if (!response.ok) {
        throw new Error('Неверный логин или пароль');
      }

      const token = await response.text();
      // Сохраняем токен в localStorage
      localStorage.setItem('adminToken', token);
      // Перенаправляем на страницу списка пользователей (создадим её позже)
      navigate('/users');
    } catch (err) {
      setError(err.message || 'Произошла ошибка при входе');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="page">
      <Header />
      <main className="main">
        <form className="login-form" onSubmit={handleSubmit}>
          <h2>Вход в систему</h2>
          
          <div className="form-group">
            <label htmlFor="login">Логин:</label>
            <input
              type="text"
              id="login"
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Пароль:</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" disabled={isLoading}>
            {isLoading ? 'Вход...' : 'Войти'}
          </button>
          {error && <div className="error-message">{error}</div>}
        </form>
      </main>
    </div>
  );
};

export default LoginPage;