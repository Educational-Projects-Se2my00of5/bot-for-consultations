import React, { useState, useEffect } from 'react';
import Header from '../components/Header';
import Modal from '../components/Modal';
import './UsersPage.css';

const DeaneryPage = () => {
  const [deaneryUsers, setDeaneryUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    fetchDeaneryUsers();
  }, []);

  const fetchDeaneryUsers = async () => {
    const token = localStorage.getItem('adminToken');
    try {
      const response = await fetch('/api/admin/unactive-deanery-accounts', {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        setDeaneryUsers(data);
      } else {
        console.error('Ошибка при получении данных о деканате');
      }
    } catch (error) {
      console.error('Ошибка:', error);
    } finally {
      setLoading(false);
    }
  };

  const getDeaneryUserDetails = async (userId) => {
    const token = localStorage.getItem('adminToken');
    try {
      const response = await fetch(`/api/admin/deanery-user-info/${userId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        setSelectedUser(data);
        setIsModalOpen(true);
      } else {
        console.error('Ошибка при получении информации о пользователе');
      }
    } catch (error) {
      console.error('Ошибка:', error);
    }
  };

  const activateDeaneryUser = async (userId) => {
    const token = localStorage.getItem('adminToken');
    try {
      const response = await fetch(`/api/admin/activate-deanery-account/${userId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        alert('Аккаунт деканата успешно активирован!');
        setIsModalOpen(false);
        fetchDeaneryUsers();
      } else {
        alert('Ошибка при активации аккаунта');
      }
    } catch (error) {
      console.error('Ошибка:', error);
      alert('Ошибка при активации аккаунта');
    }
  };

  if (loading) {
    return (
      <div className="users-page">
        <Header />
        <div className="loading">Загрузка...</div>
      </div>
    );
  }

  return (
    <div className="users-page">
      <Header />
      <div className="users-container">
        <div className="users-header">
          <h2>Список неактивных аккаунтов деканата</h2>
          <div className="header-actions">
            <button 
              className="nav-button"
              onClick={() => window.location.href = '/users'}
            >
              Преподаватели
            </button>
          </div>
        </div>
        
        {deaneryUsers.length === 0 ? (
          <div className="empty-state">
            <p>Нет неактивных аккаунтов деканата</p>
          </div>
        ) : (
          <div className="users-grid">
            {deaneryUsers.map(user => (
              <div 
                key={user.id} 
                className="user-card"
                onClick={() => getDeaneryUserDetails(user.id)}
              >
                  <h3>{(user.firstName + " " + ((user.lastName != null) ? user.lastName : "")) || 'Без имени'}</h3>
                  <p>Телефон: {user.phone || 'Не указан'}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      <Modal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)}
      >
        {selectedUser && (
          <div className="user-details">
            <h2>Информация о пользователе деканата</h2>
            <div className="detail-row">
              <strong>ФИО:</strong>
              <span>{selectedUser.lastName} {selectedUser.firstName} {selectedUser.patronymic}</span>
            </div>
            <div className="detail-row">
              <strong>Email:</strong>
              <span>{selectedUser.email || 'Не указан'}</span>
            </div>
            <div className="detail-row">
              <strong>Telegram ID:</strong>
              <span>{selectedUser.telegramId}</span>
            </div>
            <div className="detail-row">
              <strong>Номер телефона:</strong>
              <span>{selectedUser.phone || 'Не указан'}</span>
            </div>
            <div className="modal-actions">
              <button 
                className="activate-button"
                onClick={() => activateDeaneryUser(selectedUser.id)}
              >
                Активировать
              </button>
              <button 
                className="cancel-button"
                onClick={() => setIsModalOpen(false)}
              >
                Отмена
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DeaneryPage;
