import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import './Header.css';

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const isLoginPage = location.pathname === '/login';

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    navigate('/login');
  };

  return (
    <header className="header">
      <h1>Админская панель бота для консультаций</h1>
      
      {!isLoginPage && (
        <div className="header-nav">
          <button 
            className={`nav-button ${location.pathname === '/inactive' ? 'active' : ''}`}
            onClick={() => navigate('/inactive')}
          >
            Неактивные
          </button>
          <button 
            className={`nav-button ${location.pathname === '/active' ? 'active' : ''}`}
            onClick={() => navigate('/active')}
          >
            Активные
          </button>
          <button className="logout-button" onClick={handleLogout}>
            Выход
          </button>
        </div>
      )}
    </header>
  );
};

export default Header;